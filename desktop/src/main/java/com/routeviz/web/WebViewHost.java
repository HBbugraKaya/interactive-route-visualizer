package com.routeviz.web;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.service.WebAssetServer;
import com.routeviz.service.WebAssetServerFactory;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WebViewHost extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(WebViewHost.class);

    private final CanvasBridge bridge;
    private final WebAssetServerFactory webAssetServerFactory;
    private final WebStartUrlResolver startUrlResolver = new WebStartUrlResolver();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private CefApp cefApp;
    private CefClient cefClient;
    private CefBrowser cefBrowser;
    private WebAssetServer webAssetServer;
    private Consumer<String> onStatus = s -> {
    };

    public WebViewHost(CanvasBridge bridge, WebAssetServerFactory webAssetServerFactory) {
        super(new BorderLayout());
        this.bridge = bridge;
        this.webAssetServerFactory = webAssetServerFactory;
        add(loadingLabel("Starting Chromium…"), BorderLayout.CENTER);
    }

    public void setOnStatus(Consumer<String> onStatus) {
        this.onStatus = onStatus;
    }

    public CanvasBridge getBridge() {
        return bridge;
    }

    public void start(String startUrl) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        Thread starter = new Thread(() -> {
            try {
                log.info("Initializing JCEF for url={}", startUrl);
                CefAppBuilder builder = new CefAppBuilder();
                builder.setInstallDir(new File("jcef-bundle"));
                builder.getCefSettings().windowless_rendering_enabled = false;
                builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                });

                cefApp = builder.build();
                cefClient = cefApp.createClient();

                CefMessageRouter.CefMessageRouterConfig config =
                        new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
                CefMessageRouter router = CefMessageRouter.create(config);
                router.addHandler(bridge.asMessageRouterHandler(), true);
                cefClient.addMessageRouter(router);

                cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadingStateChange(CefBrowser browser, boolean isLoading,
                                                     boolean canGoBack, boolean canGoForward) {
                        if (!isLoading) {
                            SwingUtilities.invokeLater(() -> onStatus.accept("Canvas ready"));
                        }
                    }
                });

                cefBrowser = cefClient.createBrowser(startUrl, false, false);
                bridge.setBrowser(cefBrowser);
                Component ui = cefBrowser.getUIComponent();

                SwingUtilities.invokeLater(() -> {
                    removeAll();
                    add(ui, BorderLayout.CENTER);
                    revalidate();
                    repaint();
                    onStatus.accept("Loaded " + startUrl);
                });
                log.info("JCEF browser started");
            } catch (Exception e) {
                log.error("Failed to start JCEF WebView", e);
                SwingUtilities.invokeLater(() -> {
                    removeAll();
                    add(loadingLabel("Failed to start WebView: " + e.getMessage()), BorderLayout.CENTER);
                    revalidate();
                    repaint();
                    onStatus.accept("WebView error: " + e.getMessage());
                });
            }
        }, "jcef-starter");
        starter.setDaemon(true);
        starter.start();
    }

    public String resolveStartUrl() {
        WebStartUrlResolver.Resolution resolution = startUrlResolver.resolve(webAssetServerFactory, onStatus);
        if (resolution.assetServer() != null) {
            webAssetServer = resolution.assetServer();
        }
        return resolution.url();
    }

    public void disposeBrowser() {
        if (cefBrowser != null) {
            cefBrowser.close(true);
            cefBrowser = null;
        }
        if (cefClient != null) {
            cefClient.dispose();
            cefClient = null;
        }
        if (cefApp != null) {
            cefApp.dispose();
            cefApp = null;
        }
        if (webAssetServer != null) {
            webAssetServer.close();
            webAssetServer = null;
        }
        log.info("WebView disposed");
    }

    private static JLabel loadingLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        return label;
    }
}
