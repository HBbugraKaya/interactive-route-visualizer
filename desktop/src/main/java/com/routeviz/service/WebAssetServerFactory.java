package com.routeviz.service;

import java.io.IOException;

public interface WebAssetServerFactory {
    WebAssetServer start() throws IOException;
}
