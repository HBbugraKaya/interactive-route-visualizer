package com.routeviz.service;

import com.routeviz.model.WeatherDay;

import java.time.LocalDate;

public interface WeatherService {
    WeatherDay fetchDaily(double lat, double lon, LocalDate date) throws Exception;
}
