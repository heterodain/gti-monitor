package com.heterodain.gtimonitor.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heterodain.gtimonitor.config.ServiceConfig.OpenWeatherApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Open Weatherサービス
 */
@Service
@Slf4j
public class OpenWeatherService {
    /** 天気情報APIのURL */
    private static final String CURRENT_WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather?id=%s&mode=json&lang=ja&units=metric&appid=%s";
    /** HTTP接続タイムアウト(ミリ秒) */
    private static final int CONNECT_TIMEOUT = 10 * 1000; // 10秒
    /** HTTP読み込みタイムアウト(ミリ秒) */
    private static final int READ_TIMEOUT = 10 * 1000; // 10秒

    /** JSONパーサー */
    @Autowired
    private ObjectMapper om;

    /**
     * 現在の天気を取得
     * 
     * @param config API接続設定
     * @return 現在の天気
     * @throws IOException
     */
    public CurrentWeather getCurrentWeather(OpenWeatherApi config) throws IOException {
        var url = String.format(CURRENT_WEATHER_API_URL, config.getCityId(), config.getApiKey());
        log.trace("request > [GET] {}", url);

        // HTTP GET
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setDoInput(true);

        var resCode = conn.getResponseCode();
        if (resCode != 200) {
            throw new IOException("OpenWeather API Response Code " + resCode);
        }

        JsonNode json;
        try (var is = conn.getInputStream()) {
            json = om.readTree(is);
        }

        log.trace("response > {}", json);

        var result = new CurrentWeather();
        result.setWeather(json.at("/weather/0/description").textValue());
        result.setTemperature(json.at("/main/temp").doubleValue());
        result.setPressure(json.at("/main/pressure").intValue());
        result.setHumidity(json.at("/main/humidity").intValue());
        result.setWindSpeed(json.at("/wind/speed").doubleValue());
        result.setCloudness(json.at("/clouds/all").intValue());
        result.setRain1h(json.at("/rain/1h").doubleValue());
        result.setSnow1h(json.at("/snow/1h").doubleValue());

        return result;
    }

    /**
     * 現在の天気情報
     */
    @Data
    public static class CurrentWeather {
        /** 天候 */
        private String weather;
        /** 温度(℃) */
        private Double temperature;
        /** 気圧(hPa) */
        private Integer pressure;
        /** 湿度(%) */
        private Integer humidity;
        /** 風速(meter/秒) */
        private Double windSpeed;
        /** 雲量(%) */
        private Integer cloudness;
        /** 1時間当たりの降水量(mm) */
        private Double rain1h;
        /** 1時間当たりの積雪量(mm) */
        private Double snow1h;
    }
}
