package com.routeviz.bridge;

import com.routeviz.model.GraphModel;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefMessageRouterHandler;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface CanvasBridge {

    void setBrowser(CefBrowser browser);

    void setOnSelectionChanged(Consumer<GraphModel.Node> onSelectionChanged);

    void setOnStatus(Consumer<String> onStatus);

    void setOnGraphEvent(BiConsumer<String, GraphModel> onGraphEvent);

    void setOnReady(Runnable onReady);

    void setTool(String tool);

    void setDate(String isoDate);

    void setCurrencies(List<String> currencies);

    void setMeta(GraphModel.Meta meta);

    void addNode();

    void deleteSelection();

    void fitView();

    void loadGraph(GraphModel model);

    void updateSelectedNode(GraphModel.Node node);

    void requestExport();

    CefMessageRouterHandler asMessageRouterHandler();
}
