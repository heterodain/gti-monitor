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
    /** Ambientの設定 */
    private Ambient ambient;
    /** OpenWeatherの設定 */
    private OpenWeather openWeather;

    /**
     * Ambientの設定情報
     */
    @Data
    public static class Ambient {
        /** チャネルID */
        private Integer channelId;
        /** リードキー */
        private String readKey;
        /** ライトキー */
        private String writeKey;
    }

    @Data
    public static class OpenWeather {
        /** 都市ID */
        private String cityId;
        /** APIアクセスキー */
        private String apiKey;
    }
}