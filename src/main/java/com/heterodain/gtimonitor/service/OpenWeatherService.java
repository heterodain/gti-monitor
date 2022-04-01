package com.heterodain.gtimonitor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

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
    /** HTTP読み込みタイムアウト(秒) */
    private static final int READ_TIMEOUT = 10;

    /** Httpクライアント */
    @Autowired
    private HttpClient httpClient;

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
    public CurrentWeather getCurrentWeather(OpenWeatherApi config) throws IOException, InterruptedException {
        var uri = String.format(CURRENT_WEATHER_API_URL, config.getCityId(), config.getApiKey());
        log.trace("request > [GET] {}", uri);

        // HTTP GET
        var request = HttpRequest.newBuilder().GET()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(READ_TIMEOUT)).build();
        var response = httpClient.send(request, BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("OpenWeather API Response Code " + response.statusCode());
        }

        JsonNode json;
        try (var is = response.body()) {
            json = om.readTree(is);
        }
        log.trace("response > {}", json);

        // レスポンスのJSONから、天気情報を抽出
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
