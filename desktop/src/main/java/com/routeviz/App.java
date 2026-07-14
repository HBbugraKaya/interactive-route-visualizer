package com.routeviz;

import com.formdev.flatlaf.FlatDarkLaf;
import com.routeviz.config.AppContext;
import com.routeviz.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private App() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            log.warn("Could not set FlatLaf look and feel", e);
        }

        AppContext context = new AppContext();
        log.info("Interactive Route Visualizer starting");
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(context);
            frame.setVisible(true);
        });
    }
}
