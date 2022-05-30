package com.heterodain.gtimonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 制御の設定
 */
@Component
@ConfigurationProperties("control")
@Data
public class ControlConfig {
    /** 電力制御の設置絵 */
    private Power power;

    /**
     * 電力制御の設定
     */
    @Data
    public static class Power {
        /** 高電力設定プロファイル名 */
        private String highProfileName;
        /** 低電力設定プロファイル名 */
        private String lowProfileName;
        /** 電力制御に利用するデバイス */
        private Source source;
        /** Power Limit切り替え閾値(W) */
        private Integer threshold;
        /** 調整感度(W) */
        private Integer hysteresis;

        /**
         * 電力制御デバイス
         */
        public static enum Source {
            LIGHT_SENSOR, GTI;
        }
    }
}