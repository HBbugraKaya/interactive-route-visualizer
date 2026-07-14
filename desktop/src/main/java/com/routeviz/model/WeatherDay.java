package com.routeviz.model;

public final class WeatherDay {
    private final String date;
    private final double tempMax;
    private final double tempMin;
    private final int weatherCode;
    private final double precipitation;

    public WeatherDay(String date, double tempMax, double tempMin, int weatherCode, double precipitation) {
        this.date = date;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.weatherCode = weatherCode;
        this.precipitation = precipitation;
    }

    public String date() {
        return date;
    }

    public double tempMax() {
        return tempMax;
    }

    public double tempMin() {
        return tempMin;
    }

    public int weatherCode() {
        return weatherCode;
    }

    public double precipitation() {
        return precipitation;
    }
}
