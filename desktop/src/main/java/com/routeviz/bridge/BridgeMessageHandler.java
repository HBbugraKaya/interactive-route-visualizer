package com.routeviz.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.routeviz.model.GraphModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class BridgeMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(BridgeMessageHandler.class);

    private final Gson gson;
    private Consumer<GraphModel.Node> onSelectionChanged = n -> {
    };
    private Consumer<String> onStatus = s -> {
    };
    private BiConsumer<String, GraphModel> onGraphEvent = (t, g) -> {
    };
    private Runnable onReady = () -> {
    };

    public BridgeMessageHandler() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    BridgeMessageHandler(Gson gson) {
        this.gson = gson;
    }

    public void setOnSelectionChanged(Consumer<GraphModel.Node> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOnStatus(Consumer<String> onStatus) {
        this.onStatus = onStatus;
    }

    public void setOnGraphEvent(BiConsumer<String, GraphModel> onGraphEvent) {
        this.onGraphEvent = onGraphEvent;
    }

    public void setOnReady(Runnable onReady) {
        this.onReady = onReady;
    }

    public void handle(String request) {
        JsonObject msg = JsonParser.parseString(request).getAsJsonObject();
        String type = msg.get("type").getAsString();

        switch (type) {
            case "ready" -> onReady.run();
            case "status" -> {
                String text = msg.has("message") ? msg.get("message").getAsString() : "";
                onStatus.accept(text);
            }
            case "selectionChanged" -> {
                GraphModel.Node node = null;
                if (msg.has("node") && !msg.get("node").isJsonNull()) {
                    node = gson.fromJson(msg.get("node"), GraphModel.Node.class);
                }
                onSelectionChanged.accept(node);
            }
            case "graphChanged", "exportGraph" -> {
                GraphModel graph = gson.fromJson(msg.get("graph"), GraphModel.class);
                onGraphEvent.accept(type, graph);
            }
            default -> log.debug("Unknown bridge message type: {}", type);
        }
    }
}
