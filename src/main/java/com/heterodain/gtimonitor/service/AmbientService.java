package com.heterodain.gtimonitor.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heterodain.gtimonitor.config.ServiceConfig.Ambient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.var;
import lombok.extern.slf4j.Slf4j;

/**
 * Ambientサービス
 */
@Service
@Slf4j
public class AmbientService {
    /** UTCタイムゾーン */
    private static final ZoneId UTC = ZoneId.of("UTC");
    /** HTTP接続タイムアウト(ミリ秒) */
    private static final int CONNECT_TIMEOUT = 10 * 1000; // 10秒
    /** HTTP読み込みタイムアウト(ミリ秒) */
    private static final int READ_TIMEOUT = 10 * 1000; // 10秒

    /** JSONパーサー */
    @Autowired
    private ObjectMapper om;

    /** チャネルごとの前回送信時刻 */
    private Map<Integer, Long> lastSendTimes = new ConcurrentHashMap<>();

    /**
     * チャネルにデータ送信
     * 
     * @param info  チャネル情報
     * @param ts    タイムスタンプ
     * @param datas 送信データ(最大8個)
     * @throws IOException
     * @throws InterruptedException
     */
    public void send(Ambient info, ZonedDateTime ts, Double... datas)
            throws IOException, InterruptedException {

        synchronized (info) {
            // チャネルへの送信間隔が6秒以上になるように調整 (同一チャネルへの送信は5秒以上間隔を空ける必要がある)
            var lastSendTime = lastSendTimes.get(info.getChannelId());
            if (lastSendTime != null) {
                var diff = System.currentTimeMillis() - lastSendTime;
                if (diff < 6000) {
                    Thread.sleep(6000 - diff);
                }
            }

            // 送信するJSONを構築
            var rootNode = om.createObjectNode();
            rootNode.put("writeKey", info.getWriteKey());

            var dataArrayNode = om.createArrayNode();
            var dataNode = om.createObjectNode();
            var utcTs = ts.withZoneSameInstant(UTC).toLocalDateTime();
            dataNode.put("created", utcTs.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            for (int i = 1; i <= datas.length; i++) {
                if (datas[i - 1] != null) {
                    dataNode.put("d" + i, datas[i - 1]);
                }
            }
            dataArrayNode.add(dataNode);
            rootNode.set("data", dataArrayNode);

            var jsonString = om.writeValueAsString(rootNode);

            // HTTP POST
            var url = "http://ambidata.io/api/v2/channels/" + info.getChannelId() + "/dataarray";
            log.trace("request > " + url);
            log.trace("body > " + jsonString);

            var conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setDoOutput(true);
            try (var os = conn.getOutputStream()) {
                os.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
            var resCode = conn.getResponseCode();
            if (resCode != 200) {
                throw new IOException("Ambient Response Code " + resCode);
            }

            lastSendTimes.put(info.getChannelId(), System.currentTimeMillis());
        }
    }

    /**
     * 1日分のデータ取得
     * 
     * @param info チャネル情報
     * @param date 日付
     * @return 1日分のデータ
     * @throws IOException
     */
    public List<ReadData> read(Ambient info, LocalDate date) throws IOException {
        // HTTP GET
        var url = "http://ambidata.io/api/v2/channels/" + info.getChannelId() + "/data?readKey=" + info.getReadKey()
                + "&date=" + date.format(DateTimeFormatter.ISO_DATE);
        log.trace("request > " + url);

        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        var resCode = conn.getResponseCode();
        if (resCode != 200) {
            throw new IOException("Ambient Response Code " + resCode);
        }

        try (var is = conn.getInputStream()) {
            return om.readValue(is, new TypeReference<List<ReadData>>() {
            });
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReadData {
        private String created;
        private Double d1;
        private Double d2;
        private Double d3;
        private Double d4;
        private Double d5;
        private Double d6;
        private Double d7;
        private Double d8;
    }
}