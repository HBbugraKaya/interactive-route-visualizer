package com.routeviz.ui;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.config.AppContext;
import com.routeviz.model.GraphModel;
import com.routeviz.presenter.RouteEditorPresenter;
import com.routeviz.presenter.RouteEditorView;
import com.routeviz.web.WebViewHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;

public class MainFrame extends JFrame implements RouteEditorView {
    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);

    private final CanvasBridge bridge;
    private final WebViewHost webView;
    private final PropertiesPanel properties;
    private final ToolBarPanel toolBar;
    private final RouteEditorPresenter editor;
    private final JLabel statusLabel = new JLabel("Starting…", SwingConstants.LEFT);

    public MainFrame(AppContext context) {
        super("Interactive Route Visualizer");
        this.bridge = context.canvasBridge();
        this.webView = new WebViewHost(bridge, context.webAssetServerFactory());
        this.properties = new PropertiesPanel(bridge, context.geocodingService(), context.currencyService());
        this.toolBar = new ToolBarPanel(bridge);
        this.editor = new RouteEditorPresenter(context.routeDocumentService(), bridge, this);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 740));
        setLocationRelativeTo(null);

        editor.initEmptyBoard(List.of("EUR"));

        JPanel root = new JPanel(new BorderLayout());
        root.setFocusable(true);
        root.add(toolBar, BorderLayout.NORTH);
        root.add(webView, BorderLayout.CENTER);
        root.add(properties, BorderLayout.EAST);
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

        editor.wireBridge();
        wireToolbar();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                properties.dispose();
                webView.disposeBrowser();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                    root.requestFocusInWindow();
                });
            }
        });

        webView.setOnStatus(this::setStatus);
        webView.start(webView.resolveStartUrl());
        log.debug("MainFrame ready");
    }

    private void wireToolbar() {
        toolBar.setOnNew(v -> editor.newRoute());
        toolBar.setOnOpen(v -> editor.openFile());
        toolBar.setOnSave(v -> editor.requestSave());
        toolBar.setOnCurrenciesChanged(editor::onCurrenciesChanged);
    }

    @Override
    public void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    @Override
    public void showNode(GraphModel.Node node) {
        properties.showNode(node);
    }

    @Override
    public void setWeatherSummary(String text) {
        properties.setWeatherSummary(text);
    }

    @Override
    public void setPropertiesCurrencies(List<String> currencies) {
        properties.setCurrencies(currencies);
    }

    @Override
    public List<String> getToolbarCurrencies() {
        return toolBar.getMetaBar().getCurrencies();
    }

    @Override
    public void syncMetaBar(String createdAt, String updatedAt, List<String> currencies) {
        toolBar.getMetaBar().setTimestamps(createdAt, updatedAt);
        toolBar.getMetaBar().setCurrencies(currencies);
    }

    @Override
    public Path chooseOpenPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Route JSON (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().toPath();
    }

    @Override
    public Path chooseSavePath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Route JSON (*.json)", "json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().toPath();
    }

    @Override
    public boolean confirmNewRoute() {
        int result = JOptionPane.showConfirmDialog(this,
                "Clear the current route and start a new board?",
                "New route", JOptionPane.OK_CANCEL_OPTION);
        return result == JOptionPane.OK_OPTION;
    }

    @Override
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void clearGlobalFocus() {
        SwingUtilities.invokeLater(() ->
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner());
    }
}
