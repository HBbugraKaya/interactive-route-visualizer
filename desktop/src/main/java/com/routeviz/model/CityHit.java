package com.routeviz.model;

public final class CityHit {
    private final int id;
    private final String name;
    private final String country;
    private final String admin1;
    private final double latitude;
    private final double longitude;
    private final int population;

    public CityHit(
            int id,
            String name,
            String country,
            String admin1,
            double latitude,
            double longitude,
            int population) {
        this.id = id;
        this.name = name;
        this.country = country == null ? "" : country;
        this.admin1 = admin1 == null ? "" : admin1;
        this.latitude = latitude;
        this.longitude = longitude;
        this.population = population;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String country() {
        return country;
    }

    public String admin1() {
        return admin1;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public int population() {
        return population;
    }

    public String displayLabel() {
        StringBuilder sb = new StringBuilder(name);
        if (!admin1.isBlank() && !admin1.equalsIgnoreCase(name)) {
            sb.append(", ").append(admin1);
        }
        if (!country.isBlank()) {
            sb.append(" (").append(country).append(')');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
