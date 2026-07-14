package com.routeviz.ui;

import com.routeviz.bridge.CanvasBridge;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;

public class ToolBarPanel extends JPanel {
    private final CanvasBridge bridge;
    private Consumer<Void> onNew = v -> {
    };
    private Consumer<Void> onOpen = v -> {
    };
    private Consumer<Void> onSave = v -> {
    };
    private Consumer<List<String>> onCurrenciesChanged = c -> {
    };

    private final RouteMetaBar metaBar;

    public ToolBarPanel(CanvasBridge bridge) {
        super(new BorderLayout());
        this.bridge = bridge;
        this.metaBar = new RouteMetaBar();
        this.metaBar.setOnCurrenciesChanged(currencies -> {
            bridge.setCurrencies(currencies);
            onCurrenciesChanged.accept(currencies);
        });

        JToolBar fileBar = new JToolBar();
        fileBar.setFloatable(false);
        fileBar.setFocusable(false);
        fileBar.add(button("New", () -> onNew.accept(null)));
        fileBar.add(button("Open", () -> onOpen.accept(null)));
        fileBar.add(button("Save", () -> onSave.accept(null)));
        fileBar.addSeparator();
        fileBar.add(button("Add Stop", bridge::addNode));
        fileBar.add(button("Delete", bridge::deleteSelection));
        fileBar.add(button("Fit View", bridge::fitView));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tools.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        tools.add(new JLabel("Tools:"));
        ButtonGroup group = new ButtonGroup();
        tools.add(toolToggle(group, "Select", "select", true));
        tools.add(toolToggle(group, "Connect", "connect", false));
        tools.add(toolToggle(group, "Pen", "pen", false));
        tools.add(toolToggle(group, "Text", "text", false));
        tools.add(toolToggle(group, "Arrow", "arrow", false));
        tools.add(toolToggle(group, "Sticky", "sticky", false));

        JPanel top = new JPanel(new BorderLayout());
        top.add(fileBar, BorderLayout.WEST);
        top.add(metaBar, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(tools, BorderLayout.SOUTH);
    }

    public RouteMetaBar getMetaBar() {
        return metaBar;
    }

    public void setOnNew(Consumer<Void> onNew) {
        this.onNew = onNew;
    }

    public void setOnOpen(Consumer<Void> onOpen) {
        this.onOpen = onOpen;
    }

    public void setOnSave(Consumer<Void> onSave) {
        this.onSave = onSave;
    }

    public void setOnCurrenciesChanged(Consumer<List<String>> onCurrenciesChanged) {
        this.onCurrenciesChanged = onCurrenciesChanged;
    }

    private JToggleButton toolToggle(ButtonGroup group, String label, String tool, boolean selected) {
        JToggleButton btn = new JToggleButton(label, selected);
        group.add(btn);
        btn.addActionListener(e -> bridge.setTool(tool));
        return btn;
    }

    private static JButton button(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }
}
