package com.routeviz.presenter;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.model.GraphModel;
import com.routeviz.service.RouteDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RouteEditorPresenter {
    private static final Logger log = LoggerFactory.getLogger(RouteEditorPresenter.class);

    private final RouteDocumentService documents;
    private final CanvasBridge bridge;
    private final RouteEditorView view;

    private GraphModel currentGraph;
    private Path savePath;
    private boolean pendingSave;

    public RouteEditorPresenter(RouteDocumentService documents, CanvasBridge bridge, RouteEditorView view) {
        this.documents = documents;
        this.bridge = bridge;
        this.view = view;
    }

    public GraphModel currentGraph() {
        return currentGraph;
    }

    public void initEmptyBoard(List<String> currencies) {
        currentGraph = documents.createEmpty(currencies);
        view.syncMetaBar(currentGraph.meta.createdAt, currentGraph.meta.updatedAt, currencies);
        view.setPropertiesCurrencies(currencies);
    }

    public void wireBridge() {
        bridge.setOnStatus(view::setStatus);
        bridge.setOnSelectionChanged(node -> {
            view.showNode(node);
            if (node != null && node.lat != null && node.lon != null) {
                view.setWeatherSummary("see canvas badge");
            } else {
                view.setWeatherSummary("pick a city first");
            }
        });
        bridge.setOnGraphEvent((type, graph) -> {
            if (graph != null) {
                currentGraph = graph;
                documents.ensureMeta(currentGraph);
                if (!"exportGraph".equals(type)) {
                    documents.touchUpdated(currentGraph);
                }
                syncMetaBar(currentGraph);
            }
            if ("exportGraph".equals(type) && pendingSave) {
                pendingSave = false;
                doSave(graph);
            }
        });
        bridge.setOnReady(() -> {
            view.setStatus("React canvas ready");
            bridge.setCurrencies(view.getToolbarCurrencies());
            bridge.setMeta(currentGraph.meta);
            bridge.setTool("select");
            view.clearGlobalFocus();
        });
    }

    public void newRoute() {
        if (!view.confirmNewRoute()) {
            return;
        }
        savePath = null;
        currentGraph = documents.createEmpty(view.getToolbarCurrencies());
        bridge.loadGraph(currentGraph);
        syncMetaBar(currentGraph);
        view.showNode(null);
        view.setStatus("New board");
        log.info("Created new route board");
    }

    public void requestSave() {
        pendingSave = true;
        bridge.requestExport();
    }

    public void openFile() {
        Path path = view.chooseOpenPath();
        if (path == null) {
            return;
        }
        try {
            GraphModel model = documents.load(path);
            currentGraph = model;
            savePath = path;
            bridge.loadGraph(model);
            syncMetaBar(model);
            view.setPropertiesCurrencies(model.meta.currencies);
            view.setStatus("Opened " + path.getFileName());
        } catch (Exception e) {
            log.error("Failed to open {}", path, e);
            view.showError("Open error", "Failed to open: " + e.getMessage());
        }
    }

    public void onCurrenciesChanged(List<String> currencies) {
        documents.ensureMeta(currentGraph);
        currentGraph.meta.currencies = new ArrayList<>(currencies);
        documents.touchUpdated(currentGraph);
        view.setPropertiesCurrencies(currencies);
        bridge.setCurrencies(currencies);
        bridge.setMeta(currentGraph.meta);
        view.setStatus("Currencies: " + String.join(", ", currencies));
    }

    private void doSave(GraphModel graph) {
        try {
            if (savePath == null) {
                Path path = view.chooseSavePath();
                if (path == null) {
                    return;
                }
                if (!path.toString().toLowerCase().endsWith(".json")) {
                    path = Path.of(path + ".json");
                }
                savePath = path;
            }
            documents.ensureMeta(graph);
            graph.meta.currencies = new ArrayList<>(view.getToolbarCurrencies());
            documents.save(savePath, graph);
            currentGraph = graph;
            syncMetaBar(graph);
            view.setStatus("Saved " + savePath.getFileName());
        } catch (Exception e) {
            log.error("Failed to save route", e);
            view.showError("Save error", "Failed to save: " + e.getMessage());
        }
    }

    private void syncMetaBar(GraphModel graph) {
        documents.ensureMeta(graph);
        view.syncMetaBar(graph.meta.createdAt, graph.meta.updatedAt, graph.meta.currencies);
    }
}
