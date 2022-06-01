package com.heterodain.gtimonitor.task;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.heterodain.gtimonitor.config.ControlConfig;
import com.heterodain.gtimonitor.config.CostConfig;
import com.heterodain.gtimonitor.config.DeviceConfig;
import com.heterodain.gtimonitor.config.ServiceConfig;
import com.heterodain.gtimonitor.device.GridTieInverterDevice;
import com.heterodain.gtimonitor.device.LightSensorDevice;
import com.heterodain.gtimonitor.model.MeasureData;
import com.heterodain.gtimonitor.service.AmbientService;
import com.heterodain.gtimonitor.service.HiveService;
import com.heterodain.gtimonitor.service.OpenWeatherService;
import com.heterodain.gtimonitor.service.HiveService.OcProfile;
import com.heterodain.gtimonitor.service.OpenWeatherService.CurrentWeather;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * グリッドタイインバーター関連の非同期タスク
 */
@Component
@Slf4j
public class GridTieInverterTasks {
    /** 送受信リトライ回数 */
    private static final int RETRY_COUNT = 5;
    /** Ambient送受信リトライ間隔(ミリ秒) */
    private static final long AMBIENT_RETRY_INTERVAL = 5 * 60 * 1000; // 5分
    /** Hive更新リトライ間隔(ミリ秒) */
    private static final long HIVE_RETRY_INTERVAL = 1 * 60 * 1000; // 1分

    @Autowired
    private DeviceConfig deviceConfig;
    @Autowired
    private ServiceConfig serviceConfig;
    @Autowired
    private CostConfig costConfig;
    @Autowired
    private ControlConfig controlConfig;

    @Autowired
    private GridTieInverterDevice gtiDevice;
    @Autowired
    private AmbientService ambientService;
    @Autowired
    private LightSensorDevice lightSensorDevice;
    @Autowired
    private OpenWeatherService openWeatherService;
    @Autowired
    private HiveService hiveService;

    /** 照度計測データ(3秒値) */
    private List<Double> lightDatas = new ArrayList<>();
    /** GTI計測データ(30秒値) */
    private List<Double> gtiDatas = new ArrayList<>();
    /** 計測データ(3分値) */
    private List<MeasureData> threeMinDatas = new ArrayList<>();
    /** 現在のOCプロファイル */
    private OcProfile currentOcProfile;
    /** 前回の天候 */
    private String lastWeather;

    /**
     * 初期化処理
     */
    @PostConstruct
    public void init() throws IOException {
        // 照度センサー接続
        var lightSensorConfig = deviceConfig.getLightSensor();
        if (lightSensorConfig != null) {
            lightSensorDevice.connect(lightSensorConfig);
        }

        // GTI接続
        var gtiConfig = deviceConfig.getGti();
        if (gtiConfig != null) {
            gtiDevice.connect(gtiConfig);
        }
    }

    /**
     * 3秒毎に照度センサーからデータ取得
     */
    @Scheduled(initialDelay = 3, fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    public void getLightData() {
        var lightSensorConfig = deviceConfig.getLightSensor();
        if (lightSensorConfig == null) {
            return;
        }

        try {
            var current = lightSensorDevice.readLux();
            log.debug("current={} lx", current);
            synchronized (lightDatas) {
                lightDatas.add(current);
            }

        } catch (Exception e) {
            log.error("照度センサーへのアクセスに失敗しました。", e);
        }
    }

    /**
     * 30秒毎にグリッドタイインバーターからデータ取得
     */
    @Scheduled(initialDelay = 3, fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void getGtiData() {
        var gtiConfig = deviceConfig.getGti();
        if (gtiConfig == null) {
            return;
        }

        try {
            var current = gtiDevice.getCurrentPower();
            log.debug("current={} W", current);
            synchronized (gtiDatas) {
                gtiDatas.add(current);
            }

        } catch (Exception e) {
            log.error("GTIへのアクセスに失敗しました。", e);
        }
    }

    /**
     * 3分毎にAmbientにデータ送信
     */
    @Scheduled(cron = "0 */3 * * * *")
    public void sendAmbient1() throws Exception {
        if (lightDatas.isEmpty() && gtiDatas.isEmpty()) {
            return;
        }

        // 天候取得
        CurrentWeather weather;
        var openWeatherConfig = serviceConfig.getOpenWeatherApi();
        if (openWeatherConfig != null) {
            weather = openWeatherService.getCurrentWeather(openWeatherConfig);
            if (weather.getWeather().equals(lastWeather)) {
                // 前回の天候と同じ場合は、Ambientにコメントを送信しない
                weather.setWeather(null);
            } else {
                lastWeather = weather.getWeather();
            }
        } else {
            weather = new CurrentWeather();
        }

        // 平均値算出
        var average = new MeasureData();
        if (!lightDatas.isEmpty()) {
            synchronized (lightDatas) {
                average.setLight(lightDatas.stream().mapToDouble(d -> d).average().orElse(0D));
                lightDatas.clear();
            }
        }
        if (!gtiDatas.isEmpty()) {
            synchronized (gtiDatas) {
                average.setPower(gtiDatas.stream().mapToDouble(d -> d).average().orElse(0D));
                gtiDatas.clear();
            }
        }
        synchronized (threeMinDatas) {
            threeMinDatas.add(average);
        }

        // 現在のOCプロファイルをAmbientの状態色に変換(HIGH=赤[9],LOW=緑[12])
        var highProfileName = controlConfig.getPower().getHighProfileName();
        var ocState = Optional.ofNullable(currentOcProfile).map(p -> p.getName().equals(highProfileName) ? 9D : 12D)
                .orElse(null);

        // Ambient送信
        var ambientConfig = serviceConfig.getAmbient().getCurrent();
        if (ambientConfig == null) {
            return;
        }

        try {
            var sendDatas = new Double[] { average.getLight(), average.getPower(), weather.getTemperature(),
                    toDouble(weather.getCloudness()), toDouble(weather.getHumidity()), ocState };
            log.debug(
                    "Ambientに3分値を送信します。current={}lx,current={}W,weather={},temp={}℃,cloud={}%,humidity={}%,oc={}",
                    sendDatas[0], sendDatas[1], lastWeather, sendDatas[2], sendDatas[3], sendDatas[4], sendDatas[5]);

            ambientService.send(ambientConfig, ZonedDateTime.now(), weather.getWeather(), sendDatas);
        } catch (Exception e) {
            log.error("Ambientへのデータ送信に失敗しました。", e);
        }
    }

    /**
     * 15分毎にPower Limit制御
     */
    @Scheduled(cron = "10 */15 * * * *")
    public void controlPower() throws Exception {
        if (threeMinDatas.isEmpty()) {
            return;
        }

        // 平均値算出
        Double average;
        synchronized (threeMinDatas) {
            switch (controlConfig.getPower().getSource()) {
                case LIGHT_SENSOR:
                    average = threeMinDatas.stream().mapToDouble(MeasureData::getLight).average().orElse(0D);
                    break;
                case GTI:
                    average = threeMinDatas.stream().mapToDouble(MeasureData::getPower).average().orElse(0D);
                    break;
                default:
                    average = 0D;
                    break;
            }
            threeMinDatas.clear();
        }

        // OCプロファイル変更
        var hiveApiConfig = serviceConfig.getHiveApi();
        if (hiveApiConfig == null) {
            return;
        }

        var currentProfileName = currentOcProfile == null ? null : currentOcProfile.getName();
        var highProfileName = controlConfig.getPower().getHighProfileName();
        var lowProfileName = controlConfig.getPower().getLowProfileName();
        var threshold = controlConfig.getPower().getThreshold();
        var hysteresis = controlConfig.getPower().getHysteresis();

        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                // (発電電力 or 照度) > 閾値 の場合、Power Limitを上げる
                if (average > (threshold + hysteresis)
                        && (currentProfileName == null || !currentProfileName.equals(highProfileName))) {
                    log.debug("OCプロファイルを{}に変更します。", highProfileName);
                    currentOcProfile = hiveService.changeWorkerOcProfile(hiveApiConfig, highProfileName);

                    // (発電電力 or 照度) < 閾値 の場合、Power Limitを下げる
                } else if (average < (threshold - hysteresis)
                        && (currentProfileName == null || !currentProfileName.equals(lowProfileName))) {
                    log.debug("OCプロファイルを{}に変更します。", lowProfileName);
                    currentOcProfile = hiveService.changeWorkerOcProfile(hiveApiConfig, lowProfileName);
                } else {
                    log.trace("OCプロファイルの変更はありません。: {}", currentProfileName);
                }
                break;
            } catch (Exception e) {
                log.error("OCプロファイルの変更に失敗しました。", e);
            }

            // 送信失敗したら、しばらく待ってから再実行
            Thread.sleep(HIVE_RETRY_INTERVAL);
        }
    }

    /**
     * 1日毎に集計してAmbientにデータ送信
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void sendAmbient2() throws Exception {
        var ambientCurrentConfig = serviceConfig.getAmbient().getCurrent();
        var ambientSummaryConfig = serviceConfig.getAmbient().getSummary();
        if (ambientCurrentConfig == null || ambientSummaryConfig == null) {
            return;
        }

        var yesterday = LocalDate.now().minusDays(1);

        // 1日分のデータを取得して集計
        Double summary = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                // 1時間ごとの電力平均値(Wh)を算出して1日分集計
                var whs = ambientService.read(ambientCurrentConfig, yesterday).stream()
                        .filter(d -> d.getD2() != null)
                        .map(d -> Pair.of(Instant.parse(d.getCreated()).atZone(ZoneId.systemDefault()).getHour(),
                                d.getD2()))
                        .collect(Collectors.groupingBy(Pair::getKey, Collectors.averagingDouble(Pair::getValue)))
                        .values();
                summary = whs.stream().mapToDouble(d -> d).sum();
                break;
            } catch (Exception e) {
                log.error("Ambientからのデータ取得に失敗しました。", e);
            }

            // 取得失敗したら、しばらく待ってから再実行
            Thread.sleep(AMBIENT_RETRY_INTERVAL);
        }
        if (summary == null) {
            throw new IOException("Ambientからのデータ取得に失敗しました。");
        }

        // Ambient送信
        var sendDatas = new Double[] { summary, summary * costConfig.getKwh() / 1000D };
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                log.debug("Ambientに1日値を送信します。power={}Wh, yen={}", sendDatas[0], sendDatas[1]);
                ambientService.send(ambientSummaryConfig, yesterday.atStartOfDay(ZoneId.systemDefault()), null,
                        sendDatas);
                break;
            } catch (Exception e) {
                log.error("Ambientへのデータ送信に失敗しました。", e);
            }

            // 送信失敗したら、しばらく待ってから再実行
            Thread.sleep(AMBIENT_RETRY_INTERVAL);
        }
    }

    /**
     * 終了処理
     */
    @PreDestroy
    public void destroy() throws IOException {
        // GTI接続解除
        gtiDevice.close();

        // 照度センサー接続解除
        lightSensorDevice.close();
    }

    private static Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }
}
