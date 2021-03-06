package com.heterodain.gtimonitor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heterodain.gtimonitor.config.ServiceConfig.HiveApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Hiveサービス
 */
@Service
@Slf4j
public class HiveService {
    private static final String HIVE_API_BASE_URL = "https://api2.hiveos.farm/api/v2";
    private static final String GET_OC_PROFILE_URL = HIVE_API_BASE_URL + "/farms/%d/oc";
    private static final String GET_WORKER_INFO_URL = HIVE_API_BASE_URL + "/farms/%d/workers/%d";
    private static final String SET_WORKER_OC_URL = HIVE_API_BASE_URL + "/farms/%d/workers/%d";

    /** HTTP読み込みタイムアウト(秒) */
    private static final int READ_TIMEOUT = 30;

    /** Httpクライアント */
    @Autowired
    private HttpClient httpClient;

    /** JSONパーサー */
    @Autowired
    private ObjectMapper om;

    /**
     * ワーカーのOCプロファイル変更
     * 
     * @param config        Hive API接続設定
     * @param ocProfileName OCプロファイル名
     * @return 変更後のOCプロファイル
     * @throws IOException
     * @throws InterruptedException
     */
    public OcProfile changeWorkerOcProfile(HiveApi config, String ocProfileName)
            throws IOException, InterruptedException {

        var ocProfiles = getOcProfiles(config);
        log.debug("OC Profiles > {}", ocProfiles);

        var ocProfile = ocProfiles.get(ocProfileName);
        if (ocProfile == null) {
            String msg = String.format("%sに該当するOCプロファイルが定義されていません", ocProfileName);
            throw new IllegalArgumentException(msg);
        }

        // 送信JSON構築
        var rootNode = om.createObjectNode();
        rootNode.put("oc_id", ocProfile.getId());
        rootNode.put("oc_apply_mode", "replace");
        var payload = om.writeValueAsString(rootNode);

        // HTTP PATCH
        var uri = String.format(SET_WORKER_OC_URL, config.getFarmId(), config.getWorkerId());
        log.trace("request > [POST] {}", uri);
        log.trace("payload > {}", payload);

        var request = HttpRequest.newBuilder().method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(uri)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getPersonalToken())
                .timeout(Duration.ofSeconds(READ_TIMEOUT)).build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Hive API Response Code " + response.statusCode());
        }

        log.trace("response > {}", response.body());

        return ocProfile;
    }

    /**
     * ワーカー情報取得
     * 
     * @param config Hive API接続設定
     * @return ワーカー情報
     * @throws IOException
     * @throws InterruptedException
     */
    public WorkerInfo getWorkerInfo(HiveApi config) throws IOException, InterruptedException {

        // HTTP GET
        var uri = String.format(GET_WORKER_INFO_URL, config.getFarmId(), config.getWorkerId());
        log.trace("request > [GET] {}", uri);

        var request = HttpRequest.newBuilder().GET().uri(URI.create(uri))
                .header("Authorization", "Bearer " + config.getPersonalToken())
                .timeout(Duration.ofSeconds(READ_TIMEOUT)).build();
        var response = httpClient.send(request, BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Hive API Response Code " + response.statusCode());
        }

        JsonNode json;
        try (var is = response.body()) {
            json = om.readTree(is);
        }
        log.trace("response > {}", json);

        // レスポンスのJSONから、ワーカー情報を抽出
        var gpuNames = StreamSupport.stream(json.get("gpu_info").spliterator(), false)
                .collect(Collectors.toMap(
                        gpuInfo -> gpuInfo.get("bus_id").asText(),
                        gpuInfo -> gpuInfo.get("model").asText()));
        // @formatter:off
        var gpuInfos = StreamSupport.stream(json.get("gpu_stats").spliterator(), false)
                .map(gpuStats -> new GpuInfo(
                    gpuNames.get(gpuStats.get("bus_id").asText()),
                    gpuStats.get("temp").asDouble(),
                    gpuStats.get("fan").asInt(),
                    gpuStats.get("power").asInt(),
                    gpuStats.get("hash").asDouble()))
                .collect(Collectors.toList());

        return new WorkerInfo(
            json.get("id").asInt(),
            json.get("name").asText(),
            json.get("active").asBoolean(),
            json.get("oc_id").asInt(),
            json.at("/miners_summary/hashrates/0/hash").asDouble(),
            json.at("/miners_summary/hashrates/0/miner").asText(),
            json.at("/miners_summary/hashrates/0/algo").asText(),
            gpuInfos);
        // @formatter:on
    }

    /**
     * 全OCプロファイル取得
     * 
     * @param config Hive API接続設定
     * @return 全OCプロファイル
     * @throws IOException
     * @throws InterruptedException
     */
    private Map<String, OcProfile> getOcProfiles(HiveApi config) throws IOException, InterruptedException {

        // HTTP GET
        var uri = String.format(GET_OC_PROFILE_URL, config.getFarmId());
        log.trace("request > [GET] {}", uri);

        var request = HttpRequest.newBuilder().GET().uri(URI.create(uri))
                .header("Authorization", "Bearer " + config.getPersonalToken())
                .timeout(Duration.ofSeconds(READ_TIMEOUT)).build();
        var response = httpClient.send(request, BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Hive API Response Code " + response.statusCode());
        }

        JsonNode json;
        try (var is = response.body()) {
            json = om.readTree(is);
        }
        log.trace("response > {}", json);

        // レスポンスのJSONから、OCプロファイル情報を抽出
        return StreamSupport.stream(json.get("data").spliterator(), false)
                .map(oc -> new OcProfile(oc.get("id").asInt(), oc.get("name").asText(), oc.get("options")))
                .collect(Collectors.toMap(OcProfile::getName, ocp -> ocp));
    }

    /**
     * OCプロファイル情報
     */
    @AllArgsConstructor
    @Getter
    @ToString
    public static class OcProfile {
        /** プロファイルID */
        private Integer id;
        /** プロファイル名 */
        private String name;
        /** OC設定 */
        private JsonNode ocSetting;
    }

    /**
     * ワーカー情報
     */
    @AllArgsConstructor
    @Getter
    @ToString
    public static class WorkerInfo {
        /** ワーカーID */
        private Integer id;
        /** ワーカー名 */
        private String name;
        /** 稼働中かどうか */
        private Boolean active;
        /** OCプロファイルID */
        private Integer ocId;
        /** ハッシュレート */
        private Double hash;
        /** マイナー */
        private String miner;
        /** アルゴリズム */
        private String algo;
        /** GPU情報 */
        private List<GpuInfo> gpus;
    }

    /** GPU情報 */
    @AllArgsConstructor
    @Getter
    @ToString
    public static class GpuInfo {
        /** 名称 */
        public String name;
        /** 温度(℃) */
        public Double temp;
        /** ファン出力(%) */
        public Integer fan;
        /** 消費電力(W) */
        public Integer power;
        /** ハッシュレート */
        public Double hash;
    }
}
