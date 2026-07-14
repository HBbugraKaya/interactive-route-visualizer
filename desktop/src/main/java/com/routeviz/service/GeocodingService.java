package com.routeviz.service;

import com.routeviz.model.CityHit;

import java.util.List;

public interface GeocodingService {
    List<CityHit> search(String query) throws Exception;
}
