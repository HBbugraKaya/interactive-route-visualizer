package com.routeviz.ui;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.model.CityHit;
import com.routeviz.model.GraphModel;
import com.routeviz.presenter.NodePropertiesPresenter;
import com.routeviz.presenter.NodePropertiesPresenter.NodeFormState;
import com.routeviz.presenter.NodePropertiesView;
import com.routeviz.service.CurrencyService;
import com.routeviz.service.GeocodingService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class PropertiesPanel extends JPanel implements NodePropertiesView {

    private final NodePropertiesPresenter presenter;

    private final JTextField cityField = new JTextField();
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"stop", "hub"});
    private final JTextArea noteArea = new JTextArea(3, 20);

    private final JCheckBox usePeople = new JCheckBox("People");
    private final JSpinner peopleSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));

    private final JCheckBox useStayDate = new JCheckBox("Stay date");
    private final JTextField stayDateField = new JTextField();

    private final JCheckBox useNights = new JCheckBox("Hotel nights");
    private final JSpinner nightsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 365, 1));

    private final JCheckBox usePrice = new JCheckBox("€ / person / night");
    private final JSpinner priceSpinner = new JSpinner(new SpinnerNumberModel(120.0, 0.0, 100_000.0, 5.0));

    private final JLabel costPreview = new JLabel(" ");
    private final JLabel weatherLabel = new JLabel("Weather: —");
    private final JLabel hintLabel = new JLabel("Select a stop, then type a city name.");
    private final JLabel resolvedLabel = new JLabel(" ");

    private final DefaultListModel<CityHit> suggestModel = new DefaultListModel<>();
    private final JList<CityHit> suggestList = new JList<>(suggestModel);
    private final JPopupMenu suggestPopup = new JPopupMenu();
    private final Timer debounce;

    public PropertiesPanel(CanvasBridge bridge, GeocodingService geocoding, CurrencyService currencyService) {
        super(new BorderLayout(8, 8));
        this.presenter = new NodePropertiesPresenter(bridge, geocoding, currencyService, this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(300, 560));
        setFocusable(false);

        suggestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestList.setVisibleRowCount(6);
        suggestPopup.setFocusable(false);
        JScrollPane listScroll = new JScrollPane(suggestList);
        listScroll.setPreferredSize(new Dimension(260, 140));
        listScroll.setBorder(null);
        suggestPopup.add(listScroll);

        debounce = new Timer(280, e -> presenter.runCitySearch(cityField.getText()));
        debounce.setRepeats(false);

        stayDateField.setToolTipText("yyyy-MM-dd — day you will be at this stop");
        priceSpinner.setToolTipText("Stored in EUR (default 120)");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 0, 3, 4);

        addRow(form, c, new JLabel("City"), cityField);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        resolvedLabel.setFont(resolvedLabel.getFont().deriveFont(11f));
        form.add(resolvedLabel, c);

        c.gridy++;
        c.gridwidth = 1;
        addRow(form, c, new JLabel("Type"), typeBox);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        form.add(labeledCheckField(usePeople, peopleSpinner), c);
        c.gridy++;
        form.add(labeledCheckField(useStayDate, stayDateField), c);
        c.gridy++;
        form.add(labeledCheckField(useNights, nightsSpinner), c);
        c.gridy++;
        form.add(labeledCheckField(usePrice, priceSpinner), c);

        c.gridy++;
        costPreview.setFont(costPreview.getFont().deriveFont(11f));
        form.add(costPreview, c);

        c.gridy++;
        form.add(new JLabel("Note"), c);
        c.gridy++;
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        form.add(new JScrollPane(noteArea), c);

        c.gridy++;
        form.add(weatherLabel, c);
        c.gridy++;
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));
        form.add(hintLabel, c);

        add(new JLabel("Stop properties"), BorderLayout.NORTH);
        add(new JScrollPane(form), BorderLayout.CENTER);

        cityField.getDocument().addDocumentListener(simpleDoc(this::onCityTyped));
        cityField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestPopup.isVisible() || suggestModel.isEmpty()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int i = Math.min(suggestList.getSelectedIndex() + 1, suggestModel.size() - 1);
                    suggestList.setSelectedIndex(i);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int i = Math.max(suggestList.getSelectedIndex() - 1, 0);
                    suggestList.setSelectedIndex(i);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    CityHit hit = suggestList.getSelectedValue();
                    if (hit == null && !suggestModel.isEmpty()) {
                        hit = suggestModel.get(0);
                    }
                    if (hit != null) {
                        presenter.applyCity(hit);
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideSuggestions();
                    e.consume();
                }
            }
        });
        cityField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Timer t = new Timer(180, ev -> {
                    if (!suggestList.hasFocus()) {
                        hideSuggestions();
                    }
                });
                t.setRepeats(false);
                t.start();
            }
        });
        suggestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CityHit hit = suggestList.getSelectedValue();
                if (hit != null) {
                    presenter.applyCity(hit);
                }
            }
        });

        DocumentListener pushDoc = simpleDoc(this::pushFields);
        stayDateField.getDocument().addDocumentListener(pushDoc);
        noteArea.getDocument().addDocumentListener(pushDoc);
        ChangeListener pushChange = e -> pushFields();
        peopleSpinner.addChangeListener(pushChange);
        nightsSpinner.addChangeListener(pushChange);
        priceSpinner.addChangeListener(pushChange);
        typeBox.addActionListener(e -> pushFields());
        usePeople.addActionListener(e -> {
            peopleSpinner.setEnabled(usePeople.isSelected() && presenter.current() != null);
            pushFields();
        });
        useStayDate.addActionListener(e -> {
            stayDateField.setEnabled(useStayDate.isSelected() && presenter.current() != null);
            pushFields();
        });
        useNights.addActionListener(e -> {
            nightsSpinner.setEnabled(useNights.isSelected() && presenter.current() != null);
            pushFields();
        });
        usePrice.addActionListener(e -> {
            priceSpinner.setEnabled(usePrice.isSelected() && presenter.current() != null);
            pushFields();
        });

        setEnabledFields(false);
    }

    public void setCurrencies(List<String> currencies) {
        presenter.setCurrencies(currencies);
    }

    public void showNode(GraphModel.Node node) {
        presenter.showNode(node);
    }

    public void setWeatherSummary(String text) {
        weatherLabel.setText("Weather: " + (text == null || text.isBlank() ? "—" : text));
    }

    public void dispose() {
        debounce.stop();
        presenter.dispose();
    }

    private void onCityTyped() {
        if (presenter.isMute() || presenter.isApplyingSuggestion() || presenter.current() == null) {
            return;
        }
        presenter.onCityTyped(cityField.getText());
        debounce.restart();
    }

    private void pushFields() {
        if (presenter.isMute() || presenter.current() == null) {
            return;
        }
        presenter.pushFields(readForm());
    }

    @Override
    public void clearForm() {
        cityField.setText("");
        noteArea.setText("");
        typeBox.setSelectedIndex(0);
        peopleSpinner.setValue(1);
        stayDateField.setText("");
        nightsSpinner.setValue(1);
        priceSpinner.setValue(120.0);
        usePeople.setSelected(true);
        useStayDate.setSelected(true);
        useNights.setSelected(true);
        usePrice.setSelected(true);
        weatherLabel.setText("Weather: —");
        resolvedLabel.setText(" ");
        costPreview.setText(" ");
        hintLabel.setText("Select a stop, then type a city name.");
    }

    @Override
    public void loadForm(NodeFormState state) {
        cityField.setText(state.city());
        noteArea.setText(state.note());
        typeBox.setSelectedItem(state.type());
        peopleSpinner.setValue(state.peopleCount());
        stayDateField.setText(state.stayDate());
        nightsSpinner.setValue(state.hotelNights());
        priceSpinner.setValue(state.pricePerNightEur());
        usePeople.setSelected(state.usePeople());
        useStayDate.setSelected(state.useStayDate());
        useNights.setSelected(state.useHotelNights());
        usePrice.setSelected(state.usePrice());
    }

    @Override
    public NodeFormState readForm() {
        return new NodeFormState(
                cityField.getText(),
                noteArea.getText(),
                String.valueOf(typeBox.getSelectedItem()),
                (Integer) peopleSpinner.getValue(),
                stayDateField.getText(),
                (Integer) nightsSpinner.getValue(),
                ((Number) priceSpinner.getValue()).doubleValue(),
                usePeople.isSelected(),
                useStayDate.isSelected(),
                useNights.isSelected(),
                usePrice.isSelected()
        );
    }

    @Override
    public void setCityText(String text) {
        cityField.setText(text);
    }

    @Override
    public void setResolvedLabel(String text) {
        resolvedLabel.setText(text);
    }

    @Override
    public void setHint(String text) {
        hintLabel.setText(text);
    }

    @Override
    public void setCostPreview(String html) {
        costPreview.setText(html);
    }

    @Override
    public void setEnabledFields(boolean enabled) {
        cityField.setEnabled(enabled);
        typeBox.setEnabled(enabled);
        noteArea.setEnabled(enabled);
        usePeople.setEnabled(enabled);
        useStayDate.setEnabled(enabled);
        useNights.setEnabled(enabled);
        usePrice.setEnabled(enabled);
        peopleSpinner.setEnabled(enabled && usePeople.isSelected());
        stayDateField.setEnabled(enabled && useStayDate.isSelected());
        nightsSpinner.setEnabled(enabled && useNights.isSelected());
        priceSpinner.setEnabled(enabled && usePrice.isSelected());
    }

    @Override
    public void showSuggestions(List<CityHit> hits) {
        suggestModel.clear();
        hits.forEach(suggestModel::addElement);
        suggestList.setSelectedIndex(0);
        if (!cityField.isShowing() || suggestModel.isEmpty()) {
            return;
        }
        suggestPopup.setPopupSize(Math.max(220, cityField.getWidth()), Math.min(160, 24 + suggestModel.size() * 22));
        suggestPopup.show(cityField, 0, cityField.getHeight());
        cityField.requestFocusInWindow();
    }

    @Override
    public void hideSuggestions() {
        suggestPopup.setVisible(false);
        suggestModel.clear();
    }

    private static void addRow(JPanel form, GridBagConstraints c, java.awt.Component label, java.awt.Component field) {
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
        c.gridy++;
    }

    private static JPanel labeledCheckField(JCheckBox box, java.awt.Component field) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(box, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static DocumentListener simpleDoc(Runnable r) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        };
    }
}
