package com.routeviz.service;

import java.util.List;

public interface CurrencyService {
    double convertFromEur(double eurAmount, String currency);

    String format(double eurAmount, String currency);

    List<String> formatLines(double eurAmount, List<String> currencies);
}
