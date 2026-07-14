package com.routeviz.bridge;

import com.routeviz.model.GraphModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeMessageHandlerTest {

    private BridgeMessageHandler handler;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicReference<String> status = new AtomicReference<>();
    private final AtomicReference<GraphModel.Node> selection = new AtomicReference<>();
    private final List<String> graphEvents = new ArrayList<>();
    private final AtomicReference<GraphModel> lastGraph = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        handler = new BridgeMessageHandler();
        handler.setOnReady(() -> ready.set(true));
        handler.setOnStatus(status::set);
        handler.setOnSelectionChanged(selection::set);
        handler.setOnGraphEvent((type, graph) -> {
            graphEvents.add(type);
            lastGraph.set(graph);
        });
    }

    @Test
    void handle_ready() {
        handler.handle("{\"type\":\"ready\"}");
        assertTrue(ready.get());
    }

    @Test
    void handle_status() {
        handler.handle("{\"type\":\"status\",\"message\":\"Saved\"}");
        assertEquals("Saved", status.get());
    }

    @Test
    void handle_selectionChanged_withNode() {
        handler.handle("""
                {"type":"selectionChanged","node":{"id":"n1","label":"Stop A","x":10,"y":20}}
                """);
        GraphModel.Node node = selection.get();
        assertNotNull(node);
        assertEquals("n1", node.id);
        assertEquals("Stop A", node.label);
    }

    @Test
    void handle_selectionChanged_clearsSelection() {
        handler.handle("{\"type\":\"selectionChanged\",\"node\":null}");
        assertNull(selection.get());
    }

    @Test
    void handle_graphChanged() {
        handler.handle("""
                {"type":"graphChanged","graph":{"meta":{"currencies":["EUR"]},"nodes":[{"id":"a"}],"edges":[],"annotations":[]}}
                """);
        assertEquals(List.of("graphChanged"), graphEvents);
        assertNotNull(lastGraph.get());
        assertEquals(1, lastGraph.get().nodes.size());
        assertEquals("a", lastGraph.get().nodes.get(0).id);
    }

    @Test
    void handle_exportGraph() {
        handler.handle("""
                {"type":"exportGraph","graph":{"meta":{},"nodes":[],"edges":[],"annotations":[]}}
                """);
        assertEquals(List.of("exportGraph"), graphEvents);
    }

    @Test
    void handle_unknownType_isIgnored() {
        handler.handle("{\"type\":\"ping\"}");
        assertTrue(graphEvents.isEmpty());
        assertNull(status.get());
    }

    @Test
    void handle_invalidJson_throws() {
        assertThrows(Exception.class, () -> handler.handle("not-json"));
    }
}
