package com.routeviz.presenter;

import com.routeviz.model.CityHit;
import com.routeviz.presenter.NodePropertiesPresenter.NodeFormState;

import java.util.List;

public interface NodePropertiesView {

    void clearForm();

    void loadForm(NodeFormState state);

    NodeFormState readForm();

    void setCityText(String text);

    void setResolvedLabel(String text);

    void setHint(String text);

    void setCostPreview(String html);

    void setEnabledFields(boolean enabled);

    void showSuggestions(List<CityHit> hits);

    void hideSuggestions();
}
