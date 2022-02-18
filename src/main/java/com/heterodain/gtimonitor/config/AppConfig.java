package com.heterodain.gtimonitor.config;

import java.beans.BeanProperty;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * コンポーネント設定
 */
@Configuration
public class AppConfig {
    /** デフォルトのHTTPコネクションタイムアウト(秒) */
    private static final int DEFAULT_HTTP_CONNECTION_TIMEOUT = 15;

    /**
     * JSONパーサー
     * 
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * タスクスケジューラのスレッドプール
     * 
     * @return タスクスケジューラのスレッドプール
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        var taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        return taskScheduler;
    }

    /**
     * Httpクライアント
     * 
     * @return Httpクライアント
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(DEFAULT_HTTP_CONNECTION_TIMEOUT)).build();
    }
}
