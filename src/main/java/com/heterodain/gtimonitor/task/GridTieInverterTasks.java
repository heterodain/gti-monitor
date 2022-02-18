package com.heterodain.gtimonitor.task;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.heterodain.gtimonitor.config.ControlConfig;
import com.heterodain.gtimonitor.config.CostConfig;
import com.heterodain.gtimonitor.config.DeviceConfig;
import com.heterodain.gtimonitor.config.ServiceConfig;
import com.heterodain.gtimonitor.device.GridTieInverterDevice;
import com.heterodain.gtimonitor.service.AmbientService;
import com.heterodain.gtimonitor.service.HiveService;
import com.heterodain.gtimonitor.service.OpenWeatherService;
import com.heterodain.gtimonitor.service.HiveService.OcProfile;

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
    /** 送受信リトライ間隔(ミリ秒) */
    private static final long RETRY_INTERVAL = 5 * 60 * 1000; // 5分

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
    private OpenWeatherService openWeatherService;
    @Autowired
    private HiveService hiveService;

    /** 計測データ(30秒値) */
    private List<Double> thirtySecDatas = new ArrayList<>();
    /** 計測データ(3分値) */
    private List<Double> threeMinDatas = new ArrayList<>();
    /** 現在のOCプロファイル */
    private OcProfile currentOcProfile;

    /**
     * 初期化処理
     */
    @PostConstruct
    public void init() throws IOException {
        // GTI接続
        gtiDevice.connect(deviceConfig.getGti());
    }

    /**
     * 30秒毎にグリッドタイインバーターからデータ取得
     */
    @Scheduled(initialDelay = 3 * 1000, fixedDelay = 30 * 1000)
    public void realtime() {
        try {
            var current = gtiDevice.getCurrentPower(deviceConfig.getGti());
            log.debug("current={}W", current);
            synchronized (thirtySecDatas) {
                thirtySecDatas.add(current);
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
        if (thirtySecDatas.isEmpty()) {
            return;
        }

        // 天候取得
        var weather = openWeatherService.getCurrentWeather(serviceConfig.getOpenWeatherApi());

        // 平均値算出
        Double average;
        synchronized (thirtySecDatas) {
            average = thirtySecDatas.stream().mapToDouble(d -> d).average().orElse(0D);
            thirtySecDatas.clear();
        }
        synchronized (threeMinDatas) {
            threeMinDatas.add(average);
        }

        // Ambient送信
        try {
            var sendDatas = new Double[] { average, weather.getTemperature(), weather.getCloudness().doubleValue(),
                    weather.getRain1h() };
            log.debug("Ambientに3分値を送信します。current={}W,temp={}℃,cloud={}%,rain={}mm", sendDatas[0], sendDatas[1],
                    sendDatas[2], sendDatas[3] != null ? sendDatas[3] : 0);

            ambientService.send(serviceConfig.getAmbient(), ZonedDateTime.now(), weather.getWeather(), sendDatas);
        } catch (Exception e) {
            log.error("Ambientへのデータ送信に失敗しました。", e);
        }
    }

    /**
     * 15分毎に電力制御
     */
    @Scheduled(cron = "10 */15 * * * *")
    public void controlPower() throws Exception {
        if (threeMinDatas.isEmpty()) {
            return;
        }

        // 平均値算出
        Double average;
        synchronized (threeMinDatas) {
            average = threeMinDatas.stream().mapToDouble(d -> d).average().orElse(0D);
            threeMinDatas.clear();
        }

        if (average - controlConfig.getPower().getThreshold() > controlConfig.getPower().getHysteresis()) {
            // 発電電力 > 閾値 の場合、Power Limitを上げる
            if (currentOcProfile == null || "LOW".equals(currentOcProfile.getName())) {
                currentOcProfile = hiveService.changeWorkerOcProfile(serviceConfig.getHiveApi(), "HIGH");
            }

        } else if (average - controlConfig.getPower().getThreshold() < controlConfig.getPower().getHysteresis()) {
            // 発電電力 < 閾値 の場合、Power Limitを下げる
            if (currentOcProfile == null || "HIGH".equals(currentOcProfile.getName())) {
                currentOcProfile = hiveService.changeWorkerOcProfile(serviceConfig.getHiveApi(), "LOW");
            }
        }
    }

    /**
     * 1日毎に集計してAmbientにデータ送信
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void sendAmbient2() throws Exception {
        var yesterday = LocalDate.now().minusDays(1);

        // 1日分のデータを取得して集計
        Double summary = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                summary = ambientService.read(serviceConfig.getAmbient(), yesterday).stream()
                        .filter(d -> d.getD2() != null)
                        .mapToDouble(d -> d.getD2()).sum();
            } catch (Exception e) {
                log.error("Ambientからのデータ取得に失敗しました。", e);
            }

            // 送信失敗したら、しばらく待ってから再実行
            Thread.sleep(RETRY_INTERVAL);
        }
        if (summary == null) {
            throw new IOException("Ambientからのデータ取得に失敗しました。");
        }

        // Ambient送信
        var sendDatas = new Double[] { null, null, null, null, summary, summary * costConfig.getKwh() / 1000D };
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                log.debug("Ambientに1日値を送信します。power={}Wh, yen={}", sendDatas[4], sendDatas[5]);
                ambientService.send(serviceConfig.getAmbient(), yesterday.atStartOfDay(ZoneId.systemDefault()), null,
                        sendDatas);
            } catch (Exception e) {
                log.error("Ambientへのデータ送信に失敗しました。", e);
            }

            // 送信失敗したら、しばらく待ってから再実行
            Thread.sleep(RETRY_INTERVAL);
        }
    }

    /**
     * 終了処理
     */
    @PreDestroy
    public void destroy() throws IOException {
        // GTI接続解除
        gtiDevice.disconnectAll();
    }
}
