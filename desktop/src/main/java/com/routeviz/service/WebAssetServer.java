package com.routeviz.service;

public interface WebAssetServer extends AutoCloseable {
    String baseUrl();

    @Override
    void close();
}
