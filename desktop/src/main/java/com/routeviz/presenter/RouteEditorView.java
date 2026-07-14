package com.routeviz.presenter;

import com.routeviz.model.GraphModel;

import java.nio.file.Path;
import java.util.List;

public interface RouteEditorView {

    void setStatus(String text);

    void showNode(GraphModel.Node node);

    void setWeatherSummary(String text);

    void setPropertiesCurrencies(List<String> currencies);

    List<String> getToolbarCurrencies();

    void syncMetaBar(String createdAt, String updatedAt, List<String> currencies);

    Path chooseOpenPath();

    Path chooseSavePath();

    boolean confirmNewRoute();

    void showError(String title, String message);

    void clearGlobalFocus();
}
