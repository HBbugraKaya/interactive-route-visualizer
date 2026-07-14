package com.routeviz.presenter;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.model.CityHit;
import com.routeviz.model.GraphModel;
import com.routeviz.service.CurrencyService;
import com.routeviz.service.GeocodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class NodePropertiesPresenter {
    private static final Logger log = LoggerFactory.getLogger(NodePropertiesPresenter.class);

    private final CanvasBridge bridge;
    private final GeocodingService geocoding;
    private final CurrencyService currencyService;
    private final NodePropertiesView view;
    private final ExecutorService searchPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "city-search");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger searchSeq = new AtomicInteger();

    private GraphModel.Node current;
    private boolean mute;
    private boolean applyingSuggestion;
    private String lastResolvedPlace = "";
    private List<String> currencies = List.of("EUR");

    public NodePropertiesPresenter(CanvasBridge bridge,
                                   GeocodingService geocoding,
                                   CurrencyService currencyService,
                                   NodePropertiesView view) {
        this.bridge = bridge;
        this.geocoding = geocoding;
        this.currencyService = currencyService;
        this.view = view;
    }

    public boolean isMute() {
        return mute;
    }

    public boolean isApplyingSuggestion() {
        return applyingSuggestion;
    }

    public GraphModel.Node current() {
        return current;
    }

    public void setCurrencies(List<String> currencies) {
        this.currencies = currencies == null || currencies.isEmpty() ? List.of("EUR") : List.copyOf(currencies);
        refreshCostPreview();
    }

    public void showNode(GraphModel.Node node) {
        mute = true;
        view.hideSuggestions();
        current = node;
        if (node == null) {
            view.clearForm();
            lastResolvedPlace = "";
            view.setEnabledFields(false);
        } else {
            view.loadForm(new NodeFormState(
                    nullToEmpty(node.label),
                    nullToEmpty(node.note),
                    node.type == null ? "stop" : node.type,
                    node.peopleCount == null ? 1 : node.peopleCount,
                    node.stayDate == null || node.stayDate.isBlank()
                            ? LocalDate.now().toString() : node.stayDate,
                    node.hotelNights == null ? 1 : node.hotelNights,
                    node.pricePerNightEur == null ? 120.0 : node.pricePerNightEur,
                    node.usePeople == null || node.usePeople,
                    node.useStayDate == null || node.useStayDate,
                    node.useHotelNights == null || node.useHotelNights,
                    node.usePrice == null || node.usePrice
            ));
            lastResolvedPlace = nullToEmpty(node.label);
            updateResolvedHint(node);
            view.setEnabledFields(true);
            refreshCostPreview();
        }
        mute = false;
    }

    public void onCityTyped(String cityText) {
        if (mute || applyingSuggestion || current == null) {
            return;
        }
        current.label = cityText.trim();
        current.lat = null;
        current.lon = null;
        bridge.updateSelectedNode(current);
        view.setResolvedLabel("Pick a city from suggestions…");
        view.setHint("Keep typing — suggestions appear as you type.");
    }

    public void pushFields(NodeFormState state) {
        if (mute || current == null) {
            return;
        }
        current.note = state.note();
        current.type = state.type();
        current.peopleCount = state.peopleCount();
        current.stayDate = state.stayDate().trim();
        current.hotelNights = state.hotelNights();
        current.pricePerNightEur = state.pricePerNightEur();
        current.usePeople = state.usePeople();
        current.useStayDate = state.useStayDate();
        current.useHotelNights = state.useHotelNights();
        current.usePrice = state.usePrice();
        bridge.updateSelectedNode(current);
        refreshCostPreview();
    }

    public void refreshCostPreview() {
        NodeFormState state = view.readForm();
        if (current == null || state == null || !state.usePrice() || !state.useHotelNights()) {
            view.setCostPreview(" ");
            return;
        }
        int people = state.usePeople() ? state.peopleCount() : 1;
        double totalEur = people * state.hotelNights() * state.pricePerNightEur();
        List<String> lines = currencyService.formatLines(totalEur, currencies);
        view.setCostPreview("<html>Stop total:<br>" + String.join("<br>", lines) + "</html>");
    }

    public void runCitySearch(String query) {
        if (current == null) {
            return;
        }
        String trimmed = query.trim();
        if (trimmed.length() < 2) {
            view.hideSuggestions();
            return;
        }
        int seq = searchSeq.incrementAndGet();
        searchPool.submit(() -> {
            try {
                List<CityHit> hits = geocoding.search(trimmed);
                SwingUtilities.invokeLater(() -> {
                    if (seq != searchSeq.get() || current == null) {
                        return;
                    }
                    if (hits.isEmpty()) {
                        view.hideSuggestions();
                        view.setHint("No cities found for \"" + trimmed + "\".");
                        return;
                    }
                    view.showSuggestions(hits);
                    view.setHint("↑↓ to navigate, Enter to select.");
                });
            } catch (Exception ex) {
                log.warn("City search failed for '{}'", trimmed, ex);
                SwingUtilities.invokeLater(() ->
                        view.setHint("City search failed: " + ex.getMessage()));
            }
        });
    }

    public void applyCity(CityHit hit) {
        if (current == null) {
            return;
        }
        applyingSuggestion = true;
        mute = true;
        view.hideSuggestions();
        current.label = hit.name();
        current.lat = hit.latitude();
        current.lon = hit.longitude();
        view.setCityText(hit.name());
        lastResolvedPlace = hit.displayLabel();
        mute = false;
        applyingSuggestion = false;
        bridge.updateSelectedNode(current);
        updateResolvedHint(current);
        view.setHint("City selected — weather uses stay date.");
        log.debug("Applied city {}", hit.displayLabel());
    }

    public void dispose() {
        searchPool.shutdownNow();
    }

    private void updateResolvedHint(GraphModel.Node node) {
        if (node.lat != null && node.lon != null) {
            String place = lastResolvedPlace.isBlank() ? node.label : lastResolvedPlace;
            view.setResolvedLabel("Matched: " + place);
        } else {
            view.setResolvedLabel("Not matched yet");
            lastResolvedPlace = "";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public record NodeFormState(
            String city,
            String note,
            String type,
            int peopleCount,
            String stayDate,
            int hotelNights,
            double pricePerNightEur,
            boolean usePeople,
            boolean useStayDate,
            boolean useHotelNights,
            boolean usePrice
    ) {
    }
}
