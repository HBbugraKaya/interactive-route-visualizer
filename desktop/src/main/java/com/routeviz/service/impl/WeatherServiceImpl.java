package com.routeviz.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.routeviz.model.WeatherDay;
import com.routeviz.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

public final class WeatherServiceImpl implements WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);

    private final HttpClient client;

    public WeatherServiceImpl() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    WeatherServiceImpl(HttpClient client) {
        this.client = client;
    }

    @Override
    public WeatherDay fetchDaily(double lat, double lon, LocalDate date) throws Exception {
        LocalDate today = LocalDate.now();
        boolean archive = date.isBefore(today) || date.isAfter(today.plusDays(15));
        String base = archive
                ? "https://archive-api.open-meteo.com/v1/archive"
                : "https://api.open-meteo.com/v1/forecast";

        String url = String.format(
                "%s?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,weathercode,precipitation_sum&timezone=auto&start_date=%s&end_date=%s",
                base, lat, lon, date, date);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Open-Meteo HTTP " + response.statusCode());
        }

        JsonObject daily = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("daily");
        WeatherDay day = new WeatherDay(
                date.toString(),
                firstDouble(daily.getAsJsonArray("temperature_2m_max")),
                firstDouble(daily.getAsJsonArray("temperature_2m_min")),
                (int) firstDouble(daily.getAsJsonArray("weathercode")),
                firstDouble(daily.getAsJsonArray("precipitation_sum"))
        );
        log.debug("Weather {} @ {},{} -> max={}", date, lat, lon, day.tempMax());
        return day;
    }

    private static double firstDouble(JsonArray arr) {
        if (arr == null || arr.isEmpty() || arr.get(0).isJsonNull()) {
            return Double.NaN;
        }
        return arr.get(0).getAsDouble();
    }
}
