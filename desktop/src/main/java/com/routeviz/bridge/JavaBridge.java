package com.routeviz.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.routeviz.model.GraphModel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JavaBridge extends CefMessageRouterHandlerAdapter implements CanvasBridge {
    private static final Logger log = LoggerFactory.getLogger(JavaBridge.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final BridgeMessageHandler messages = new BridgeMessageHandler(gson);
    private CefBrowser browser;

    @Override
    public void setBrowser(CefBrowser browser) {
        this.browser = browser;
    }

    @Override
    public void setOnSelectionChanged(Consumer<GraphModel.Node> onSelectionChanged) {
        messages.setOnSelectionChanged(node ->
                SwingUtilities.invokeLater(() -> onSelectionChanged.accept(node)));
    }

    @Override
    public void setOnStatus(Consumer<String> onStatus) {
        messages.setOnStatus(text -> SwingUtilities.invokeLater(() -> onStatus.accept(text)));
    }

    @Override
    public void setOnGraphEvent(BiConsumer<String, GraphModel> onGraphEvent) {
        messages.setOnGraphEvent((type, graph) ->
                SwingUtilities.invokeLater(() -> onGraphEvent.accept(type, graph)));
    }

    @Override
    public void setOnReady(Runnable onReady) {
        messages.setOnReady(() -> SwingUtilities.invokeLater(onReady));
    }

    public void sendCommand(String type, Object payload) {
        if (browser == null) {
            log.debug("Skip command '{}': browser not ready", type);
            return;
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        if (payload != null) {
            msg.add("payload", JsonParser.parseString(gson.toJson(payload)));
        }
        String js = "window.routeViz && window.routeViz.command(" + msg + ");";
        SwingUtilities.invokeLater(() -> browser.executeJavaScript(js, browser.getURL(), 0));
    }

    @Override
    public void setTool(String tool) {
        sendCommand("setTool", tool);
    }

    @Override
    public void setDate(String isoDate) {
        sendCommand("setDate", isoDate);
    }

    @Override
    public void setCurrencies(List<String> currencies) {
        sendCommand("setCurrencies", currencies);
    }

    @Override
    public void setMeta(GraphModel.Meta meta) {
        sendCommand("setMeta", meta);
    }

    @Override
    public void addNode() {
        sendCommand("addNode", null);
    }

    @Override
    public void deleteSelection() {
        sendCommand("deleteSelection", null);
    }

    @Override
    public void fitView() {
        sendCommand("fitView", null);
    }

    @Override
    public void loadGraph(GraphModel model) {
        sendCommand("loadGraph", model);
    }

    @Override
    public void updateSelectedNode(GraphModel.Node node) {
        sendCommand("updateNode", node);
    }

    @Override
    public void requestExport() {
        sendCommand("exportGraph", null);
    }

    @Override
    public CefMessageRouterHandler asMessageRouterHandler() {
        return this;
    }

    @Override
    public boolean onQuery(CefBrowser cefBrowser, CefFrame frame, long queryId, String request,
                           boolean persistent, CefQueryCallback callback) {
        try {
            messages.handle(request);
            callback.success("ok");
        } catch (Exception e) {
            log.warn("Bridge query failed: {}", request, e);
            callback.failure(1, e.getMessage() == null ? "bridge error" : e.getMessage());
        }
        return true;
    }
}
