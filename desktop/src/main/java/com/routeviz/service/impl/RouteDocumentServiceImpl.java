package com.routeviz.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.routeviz.model.GraphModel;
import com.routeviz.service.RouteDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RouteDocumentServiceImpl implements RouteDocumentService {
    private static final Logger log = LoggerFactory.getLogger(RouteDocumentServiceImpl.class);

    private final Gson gson;

    public RouteDocumentServiceImpl() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    RouteDocumentServiceImpl(Gson gson) {
        this.gson = gson;
    }

    @Override
    public GraphModel createEmpty(List<String> currencies) {
        GraphModel empty = new GraphModel();
        String today = LocalDate.now().toString();
        empty.meta.createdAt = today;
        empty.meta.updatedAt = today;
        empty.meta.currencies = new ArrayList<>(
                currencies == null || currencies.isEmpty() ? List.of("EUR") : currencies);
        return empty;
    }

    @Override
    public void ensureMeta(GraphModel graph) {
        if (graph.meta == null) {
            graph.meta = new GraphModel.Meta();
        }
        String today = LocalDate.now().toString();
        if (graph.meta.createdAt == null || graph.meta.createdAt.isBlank()) {
            graph.meta.createdAt = graph.meta.date != null && !graph.meta.date.isBlank()
                    ? graph.meta.date : today;
        }
        if (graph.meta.updatedAt == null || graph.meta.updatedAt.isBlank()) {
            graph.meta.updatedAt = graph.meta.createdAt;
        }
        if (graph.meta.currencies == null || graph.meta.currencies.isEmpty()) {
            graph.meta.currencies = new ArrayList<>(List.of("EUR"));
        }
    }

    @Override
    public void touchUpdated(GraphModel graph) {
        ensureMeta(graph);
        graph.meta.updatedAt = LocalDate.now().toString();
    }

    @Override
    public GraphModel load(Path path) throws Exception {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        GraphModel model = gson.fromJson(json, GraphModel.class);
        if (model == null) {
            throw new IOException("Empty or invalid JSON");
        }
        ensureMeta(model);
        log.info("Loaded route from {}", path.getFileName());
        return model;
    }

    @Override
    public void save(Path path, GraphModel graph) throws Exception {
        ensureMeta(graph);
        if (graph.meta.createdAt == null || graph.meta.createdAt.isBlank()) {
            graph.meta.createdAt = LocalDate.now().toString();
        }
        graph.meta.updatedAt = LocalDate.now().toString();
        Files.writeString(path, gson.toJson(graph), StandardCharsets.UTF_8);
        log.info("Saved route to {}", path.getFileName());
    }
}
