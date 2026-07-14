package com.routeviz.service.impl;

import com.routeviz.model.GraphModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteDocumentServiceImplTest {

    private final RouteDocumentServiceImpl service = new RouteDocumentServiceImpl();

    @TempDir
    Path tempDir;

    @Test
    void createEmpty_setsTodayAndDefaultCurrency() {
        GraphModel empty = service.createEmpty(null);
        String today = LocalDate.now().toString();

        assertEquals(today, empty.meta.createdAt);
        assertEquals(today, empty.meta.updatedAt);
        assertEquals(List.of("EUR"), empty.meta.currencies);
        assertTrue(empty.nodes.isEmpty());
    }

    @Test
    void createEmpty_usesProvidedCurrencies() {
        GraphModel empty = service.createEmpty(List.of("USD", "TRY"));
        assertEquals(List.of("USD", "TRY"), empty.meta.currencies);
    }

    @Test
    void ensureMeta_fillsMissingFields() {
        GraphModel graph = new GraphModel();
        graph.meta = null;

        service.ensureMeta(graph);

        assertNotNull(graph.meta);
        assertEquals(LocalDate.now().toString(), graph.meta.createdAt);
        assertEquals(graph.meta.createdAt, graph.meta.updatedAt);
        assertEquals(List.of("EUR"), graph.meta.currencies);
    }

    @Test
    void ensureMeta_migratesLegacyDate() {
        GraphModel graph = new GraphModel();
        graph.meta.createdAt = null;
        graph.meta.updatedAt = null;
        graph.meta.date = "2024-06-15";
        graph.meta.currencies = List.of();

        service.ensureMeta(graph);

        assertEquals("2024-06-15", graph.meta.createdAt);
        assertEquals("2024-06-15", graph.meta.updatedAt);
        assertEquals(List.of("EUR"), graph.meta.currencies);
    }

    @Test
    void touchUpdated_refreshesUpdatedAt() {
        GraphModel graph = service.createEmpty(List.of("EUR"));
        graph.meta.updatedAt = "2000-01-01";

        service.touchUpdated(graph);

        assertEquals(LocalDate.now().toString(), graph.meta.updatedAt);
    }

    @Test
    void saveAndLoad_roundTrip() throws Exception {
        Path file = tempDir.resolve("route.json");
        GraphModel original = service.createEmpty(List.of("EUR", "USD"));
        GraphModel.Node node = new GraphModel.Node();
        node.id = "n1";
        node.label = "Istanbul";
        node.lat = 41.0;
        node.lon = 29.0;
        original.nodes.add(node);

        service.save(file, original);
        GraphModel loaded = service.load(file);

        assertEquals(1, loaded.nodes.size());
        assertEquals("n1", loaded.nodes.get(0).id);
        assertEquals("Istanbul", loaded.nodes.get(0).label);
        assertEquals(List.of("EUR", "USD"), loaded.meta.currencies);
        assertNotNull(loaded.meta.createdAt);
        assertNotNull(loaded.meta.updatedAt);
    }

    @Test
    void load_rejectsInvalidJson() throws Exception {
        Path file = tempDir.resolve("bad.json");
        Files.writeString(file, "not-json{{{");

        assertThrows(Exception.class, () -> service.load(file));
    }

    @Test
    void load_rejectsEmptyJsonNull() throws Exception {
        Path file = tempDir.resolve("null.json");
        Files.writeString(file, "null");

        Exception ex = assertThrows(Exception.class, () -> service.load(file));
        assertTrue(ex.getMessage().toLowerCase().contains("empty")
                || ex.getMessage().toLowerCase().contains("invalid"));
    }
}
