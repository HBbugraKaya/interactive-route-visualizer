package com.routeviz.config;

import com.routeviz.bridge.CanvasBridge;
import com.routeviz.bridge.JavaBridge;
import com.routeviz.service.CurrencyService;
import com.routeviz.service.GeocodingService;
import com.routeviz.service.RouteDocumentService;
import com.routeviz.service.WebAssetServerFactory;
import com.routeviz.service.impl.CurrencyServiceImpl;
import com.routeviz.service.impl.GeocodingServiceImpl;
import com.routeviz.service.impl.RouteDocumentServiceImpl;
import com.routeviz.service.impl.WebAssetServerImpl;

public final class AppContext {
    private final CurrencyService currencyService;
    private final GeocodingService geocodingService;
    private final RouteDocumentService routeDocumentService;
    private final WebAssetServerFactory webAssetServerFactory;
    private final CanvasBridge canvasBridge;

    public AppContext() {
        this.currencyService = new CurrencyServiceImpl();
        this.geocodingService = new GeocodingServiceImpl();
        this.routeDocumentService = new RouteDocumentServiceImpl();
        this.webAssetServerFactory = WebAssetServerImpl.factory();
        this.canvasBridge = new JavaBridge();
    }

    public CurrencyService currencyService() {
        return currencyService;
    }

    public GeocodingService geocodingService() {
        return geocodingService;
    }

    public RouteDocumentService routeDocumentService() {
        return routeDocumentService;
    }

    public WebAssetServerFactory webAssetServerFactory() {
        return webAssetServerFactory;
    }

    public CanvasBridge canvasBridge() {
        return canvasBridge;
    }
}
