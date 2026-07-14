package com.routeviz.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.routeviz.model.CityHit;
import com.routeviz.service.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeocodingServiceImpl implements GeocodingService {
    private static final Logger log = LoggerFactory.getLogger(GeocodingServiceImpl.class);
    private static final List<String> LANGUAGES = List.of("en", "tr", "fr");
    private static final int MAX_RESULTS = 8;

    private final HttpClient client;

    public GeocodingServiceImpl() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
    }

    GeocodingServiceImpl(HttpClient client) {
        this.client = client;
    }

    @Override
    public List<CityHit> search(String query) throws Exception {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }

        Map<Integer, CityHit> byId = new LinkedHashMap<>();
        for (String lang : LANGUAGES) {
            for (CityHit hit : fetch(q, lang)) {
                byId.putIfAbsent(hit.id(), hit);
            }
            if (byId.size() >= MAX_RESULTS) {
                break;
            }
        }
        List<CityHit> results = new ArrayList<>(byId.values());
        log.debug("Geocode '{}' -> {} hit(s)", q, results.size());
        return results.subList(0, Math.min(MAX_RESULTS, results.size()));
    }

    private List<CityHit> fetch(String query, String language) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + encoded + "&count=8&language=" + language + "&format=json";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Geocoding HTTP {} for query '{}' lang={}", response.statusCode(), query, language);
            return List.of();
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("results") || root.get("results").isJsonNull()) {
            return List.of();
        }

        JsonArray results = root.getAsJsonArray("results");
        List<CityHit> hits = new ArrayList<>();
        for (JsonElement el : results) {
            JsonObject o = el.getAsJsonObject();
            String name = text(o, "name");
            double lat = o.has("latitude") ? o.get("latitude").getAsDouble() : Double.NaN;
            double lon = o.has("longitude") ? o.get("longitude").getAsDouble() : Double.NaN;
            if (name.isBlank() || Double.isNaN(lat) || Double.isNaN(lon)) {
                continue;
            }
            int id = o.has("id") ? o.get("id").getAsInt() : System.identityHashCode(o);
            int population = o.has("population") && !o.get("population").isJsonNull()
                    ? o.get("population").getAsInt() : 0;
            hits.add(new CityHit(id, name, text(o, "country"), text(o, "admin1"), lat, lon, population));
        }
        return hits;
    }

    private static String text(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        return o.get(key).getAsString();
    }
}
