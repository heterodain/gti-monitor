package com.heterodain.gtimonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 費用の設定
 */
@Component
@ConfigurationProperties("cost")
@Data
public class CostConfig {
    /** 1kWh当たりの電気代(円) */
    private Double kwh;
}