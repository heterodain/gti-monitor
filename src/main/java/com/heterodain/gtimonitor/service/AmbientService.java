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
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Autowired
    private ObjectMapper om;

    /**
     * チャネルにデータ送信
     * 
     * @param info  チャネル情報
     * @param ts    タイムスタンプ
     * @param datas 送信データ(最大8個)
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized void send(Ambient info, ZonedDateTime ts, Double... datas)
            throws IOException, InterruptedException {

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
        var url = "http://54.65.206.59/api/v2/channels/" + info.getChannelId() + "/dataarray";
        log.trace("request > " + url);
        log.trace("body > " + jsonString);

        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        try (var os = conn.getOutputStream()) {
            os.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }
        var resCode = conn.getResponseCode();
        if (resCode != 200) {
            throw new IOException("Ambient Response Code " + resCode);
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
        var url = "http://54.65.206.59/api/v2/channels/" + info.getChannelId() + "/data?readKey=" + info.getReadKey()
                + "&date=" + date.format(DateTimeFormatter.ISO_DATE);
        log.trace("request > " + url);

        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
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