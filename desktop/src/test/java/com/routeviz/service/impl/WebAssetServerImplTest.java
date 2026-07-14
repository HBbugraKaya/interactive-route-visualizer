package com.routeviz.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAssetServerImplTest {

    @Test
    void contentTypeFor_knownExtensions() {
        assertEquals("application/javascript; charset=utf-8", WebAssetServerImpl.contentTypeFor("/app.js"));
        assertEquals("text/css; charset=utf-8", WebAssetServerImpl.contentTypeFor("/styles.css"));
        assertEquals("text/html; charset=utf-8", WebAssetServerImpl.contentTypeFor("/index.html"));
        assertEquals("image/svg+xml", WebAssetServerImpl.contentTypeFor("/icon.svg"));
        assertEquals("application/json; charset=utf-8", WebAssetServerImpl.contentTypeFor("/data.json"));
    }

    @Test
    void contentTypeFor_unknownFallsBack() {
        String type = WebAssetServerImpl.contentTypeFor("/file.bin");
        assertTrue(type != null && !type.isBlank());
    }
}
