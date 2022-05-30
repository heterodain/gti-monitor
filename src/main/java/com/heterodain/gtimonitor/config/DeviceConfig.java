package com.heterodain.gtimonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * デバイスの設定
 */
@Component
@ConfigurationProperties("device")
@Data
public class DeviceConfig {
    /* 照度センサーの設定 */
    private LightSensor lightSensor;
    /* GTIの設定 */
    private Gti gti;

    /**
     * 照度センサーの設定情報
     */
    @Data
    public static class LightSensor {
        /* シリアル通信ポート名 */
        private String comPort;
    }

    /**
     * GTIの設定情報
     */
    @Data
    public static class Gti {
        /* シリアル通信ポート名 */
        private String comPort;
        /* ModbusのユニットID */
        private Integer unitId;
    }
}