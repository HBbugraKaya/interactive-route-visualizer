package com.routeviz.ui;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RouteMetaBar extends JPanel {
    private final JLabel createdLabel = new JLabel("Created: —");
    private final JLabel updatedLabel = new JLabel("Updated: —");
    private final JCheckBox eur = new JCheckBox("EUR", true);
    private final JCheckBox tryBox = new JCheckBox("TRY", false);
    private final JCheckBox usd = new JCheckBox("USD", false);

    private Consumer<List<String>> onCurrenciesChanged = c -> {
    };

    public RouteMetaBar() {
        super(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        setFocusable(false);
        createdLabel.setFocusable(false);
        updatedLabel.setFocusable(false);

        add(createdLabel);
        add(updatedLabel);
        add(new JLabel("Currency:"));
        add(eur);
        add(tryBox);
        add(usd);

        eur.addActionListener(e -> onToggle(eur));
        tryBox.addActionListener(e -> onToggle(tryBox));
        usd.addActionListener(e -> onToggle(usd));
    }

    public void setOnCurrenciesChanged(Consumer<List<String>> onCurrenciesChanged) {
        this.onCurrenciesChanged = onCurrenciesChanged;
    }

    public void setCreated(String iso) {
        createdLabel.setText("Created: " + (iso == null || iso.isBlank() ? "—" : iso));
    }

    public void setUpdated(String iso) {
        updatedLabel.setText("Updated: " + (iso == null || iso.isBlank() ? "—" : iso));
    }

    public void setTimestamps(String created, String updated) {
        setCreated(created);
        setUpdated(updated);
    }

    public void setCurrencies(List<String> currencies) {
        Set<String> set = new LinkedHashSet<>();
        if (currencies != null) {
            set.addAll(currencies);
        }
        if (set.isEmpty()) {
            set.add("EUR");
        }
        eur.setSelected(set.contains("EUR"));
        tryBox.setSelected(set.contains("TRY"));
        usd.setSelected(set.contains("USD"));
        if (!eur.isSelected() && !tryBox.isSelected() && !usd.isSelected()) {
            eur.setSelected(true);
        }
    }

    public List<String> getCurrencies() {
        List<String> list = new ArrayList<>();
        if (eur.isSelected()) {
            list.add("EUR");
        }
        if (tryBox.isSelected()) {
            list.add("TRY");
        }
        if (usd.isSelected()) {
            list.add("USD");
        }
        if (list.isEmpty()) {
            list.add("EUR");
            eur.setSelected(true);
        }
        return list;
    }

    public void touchUpdatedNow() {
        setUpdated(LocalDate.now().toString());
    }

    private void onToggle(JCheckBox source) {
        if (!eur.isSelected() && !tryBox.isSelected() && !usd.isSelected()) {
            source.setSelected(true);
            JOptionPane.showMessageDialog(this,
                    "En az bir para birimi seçili olmalı.",
                    "Currency",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        onCurrenciesChanged.accept(getCurrencies());
    }
}
