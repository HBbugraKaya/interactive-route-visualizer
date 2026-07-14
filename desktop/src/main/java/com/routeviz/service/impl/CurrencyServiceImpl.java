package com.routeviz.service.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.routeviz.service.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CurrencyServiceImpl implements CurrencyService {
    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceImpl.class);
    private static final long STALE_MS = 60 * 60 * 1000L;
    private static final Map<String, Double> FALLBACK = Map.of(
            "EUR", 1.0,
            "USD", 1.14,
            "TRY", 53.7
    );

    private final HttpClient client;
    private final Map<String, Double> ratesFromEur = new LinkedHashMap<>(FALLBACK);
    private long fetchedAt;

    public CurrencyServiceImpl() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build());
    }

    CurrencyServiceImpl(HttpClient client) {
        this.client = client;
    }

    private synchronized void refreshIfStale() {
        if (System.currentTimeMillis() - fetchedAt < STALE_MS) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("https://api.frankfurter.app/latest?from=EUR&to=USD,TRY"))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Currency refresh failed with HTTP {}", response.statusCode());
                return;
            }
            JsonObject rates = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("rates");
            ratesFromEur.put("EUR", 1.0);
            if (rates.has("USD")) {
                ratesFromEur.put("USD", rates.get("USD").getAsDouble());
            }
            if (rates.has("TRY")) {
                ratesFromEur.put("TRY", rates.get("TRY").getAsDouble());
            }
            fetchedAt = System.currentTimeMillis();
            log.info("Currency rates refreshed from Frankfurter");
        } catch (Exception e) {
            log.warn("Keeping fallback currency rates: {}", e.toString());
        }
    }

    @Override
    public double convertFromEur(double eurAmount, String currency) {
        refreshIfStale();
        double rate = ratesFromEur.getOrDefault(currency, FALLBACK.getOrDefault(currency, 1.0));
        return eurAmount * rate;
    }

    @Override
    public String format(double eurAmount, String currency) {
        double value = convertFromEur(eurAmount, currency);
        String symbol = switch (currency) {
            case "TRY" -> "₺";
            case "USD" -> "$";
            default -> "€";
        };
        return String.format(Locale.US, "%s%.2f", symbol, value);
    }

    @Override
    public List<String> formatLines(double eurAmount, List<String> currencies) {
        return currencies.stream().map(c -> format(eurAmount, c)).toList();
    }
}
