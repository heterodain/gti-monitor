package com.heterodain.gtimonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * コンポーネント設定
 */
@Configuration
public class AppConfig {

    /**
     * JSONパーサーをDIコンテナに登録
     * 
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}
