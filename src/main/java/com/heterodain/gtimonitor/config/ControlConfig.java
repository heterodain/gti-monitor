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
    /** 電力 */
    private Power power;

    /**
     * 電力制御の設定
     */
    @Data
    public static class Power {
        /** Power Limit切り替え閾値(W) */
        private Integer threshold;
        /** 調整感度(W) */
        private Integer hysteresis;
    }
}