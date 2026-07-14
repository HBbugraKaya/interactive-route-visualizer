package com.routeviz.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private CurrencyServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("offline"));
        service = new CurrencyServiceImpl(httpClient);
    }

    @Test
    void convertFromEur_usesFallbackRates() {
        assertEquals(1.0, service.convertFromEur(1.0, "EUR"), 1e-9);
        assertEquals(1.14, service.convertFromEur(1.0, "USD"), 1e-9);
        assertEquals(53.7, service.convertFromEur(1.0, "TRY"), 1e-9);
        assertEquals(11.4, service.convertFromEur(10.0, "USD"), 1e-9);
    }

    @Test
    void format_usesCurrencySymbols() {
        assertEquals("€10.00", service.format(10.0, "EUR"));
        assertEquals("$11.40", service.format(10.0, "USD"));
        assertEquals("₺537.00", service.format(10.0, "TRY"));
    }

    @Test
    void formatLines_formatsEachCurrency() {
        List<String> lines = service.formatLines(1.0, List.of("EUR", "USD"));
        assertEquals(List.of("€1.00", "$1.14"), lines);
    }

    @Test
    void convertFromEur_unknownCurrencyDefaultsToOne() {
        assertEquals(5.0, service.convertFromEur(5.0, "XYZ"), 1e-9);
    }
}
