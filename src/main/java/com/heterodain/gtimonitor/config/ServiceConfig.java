package com.heterodain.gtimonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * サービスの設定
 */
@Component
@ConfigurationProperties("service")
@Data
public class ServiceConfig {
    /** Ambientのチャネル設定 */
    private AmbientChannel ambient;
    /** Open Weather APIの設定 */
    private OpenWeatherApi openWeatherApi;
    /** Hive APIの設定 */
    private HiveApi hiveApi;

    /**
     * Ambientのチャネル設定情報
     */
    @Data
    public static class AmbientChannel {
        /** 現在値 */
        private AmbientApi current;
        /** 集計値 */
        private AmbientApi summary;
    }

    /**
     * AmbientのAPI設定情報
     */
    @Data
    public static class AmbientApi {
        /** チャネルID */
        private Integer channelId;
        /** リードキー */
        private String readKey;
        /** ライトキー */
        private String writeKey;
    }

    /**
     * Open Weather APIの接続設定
     */
    @Data
    public static class OpenWeatherApi {
        /** 都市ID */
        private String cityId;
        /** APIアクセスキー */
        private String apiKey;
    }

    @Data
    public static class HiveApi {
        /** ファームID */
        private Integer farmId;
        /** ワーカーID */
        private Integer workerId;
        /** パーソナルAPIトークン */
        private String personalToken;
    }
}