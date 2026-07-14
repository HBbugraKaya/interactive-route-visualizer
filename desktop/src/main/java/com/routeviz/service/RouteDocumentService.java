package com.routeviz.service;

import com.routeviz.model.GraphModel;

import java.nio.file.Path;
import java.util.List;

public interface RouteDocumentService {
    GraphModel createEmpty(List<String> currencies);

    void ensureMeta(GraphModel graph);

    void touchUpdated(GraphModel graph);

    GraphModel load(Path path) throws Exception;

    void save(Path path, GraphModel graph) throws Exception;
}
