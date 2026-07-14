package com.routeviz.service.impl;

import com.routeviz.model.CityHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceImplTest {

    private static final String SAMPLE_JSON = """
            {
              "results": [
                {
                  "id": 745044,
                  "name": "Istanbul",
                  "latitude": 41.01384,
                  "longitude": 28.94966,
                  "country": "Turkey",
                  "admin1": "Istanbul",
                  "population": 14804116
                },
                {
                  "id": 323786,
                  "name": "Ankara",
                  "latitude": 39.91987,
                  "longitude": 32.85427,
                  "country": "Turkey",
                  "admin1": "Ankara",
                  "population": 3517182
                }
              ]
            }
            """;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private GeocodingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GeocodingServiceImpl(httpClient);
    }

    @Test
    void search_blankOrShortQuery_returnsEmpty() throws Exception {
        assertTrue(service.search(null).isEmpty());
        assertTrue(service.search("").isEmpty());
        assertTrue(service.search(" ").isEmpty());
        assertTrue(service.search("a").isEmpty());
    }

    @Test
    void search_parsesOpenMeteoHits() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(SAMPLE_JSON);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<CityHit> hits = service.search("Istanbul");

        assertEquals(2, hits.size());
        CityHit first = hits.get(0);
        assertEquals(745044, first.id());
        assertEquals("Istanbul", first.name());
        assertEquals("Turkey", first.country());
        assertEquals(41.01384, first.latitude(), 1e-5);
        assertEquals(28.94966, first.longitude(), 1e-5);
        assertEquals(14804116, first.population());
        assertEquals("Ankara", hits.get(1).name());
    }

    @Test
    void search_non200_returnsEmpty() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertTrue(service.search("Paris").isEmpty());
    }
}
