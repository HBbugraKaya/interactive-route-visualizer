package com.routeviz.model;

import java.util.ArrayList;
import java.util.List;

public class GraphModel {
    public Meta meta = new Meta();
    public List<Node> nodes = new ArrayList<>();
    public List<Edge> edges = new ArrayList<>();
    public List<Annotation> annotations = new ArrayList<>();

    public static class Meta {
        public String date;
        public String createdAt;
        public String updatedAt;

        public List<String> currencies = new ArrayList<>(List.of("EUR"));
    }

    public static class Node {
        public String id;
        public double x;
        public double y;
        public String label;
        public Double lat;
        public Double lon;
        public String type = "stop";
        public String note = "";

        public Integer peopleCount = 1;
        public String stayDate;
        public Integer hotelNights = 1;

        public Double pricePerNightEur = 120.0;

        public Boolean usePeople = true;
        public Boolean useStayDate = true;
        public Boolean useHotelNights = true;
        public Boolean usePrice = true;
    }

    public static class Edge {
        public String id;
        public String from;
        public String to;
    }

    public static class Annotation {
        public String id;
        public String type;
        public String color;
        public String text;
        public Double x;
        public Double y;
        public Double x2;
        public Double y2;
        public List<List<Double>> points;
    }
}
