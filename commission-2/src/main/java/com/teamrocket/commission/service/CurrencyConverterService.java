package com.teamrocket.commission.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyConverterService {

    // Hardcoded conversion rates relative to your base currency (INR)
    private final Map<String, Double> exchangeRates;

    public CurrencyConverterService() {
        exchangeRates = new HashMap<>();
        exchangeRates.put("INR", 1.0);      // Base Currency
        exchangeRates.put("USD", 0.012);    // 1 INR = 0.012 USD
        exchangeRates.put("EUR", 0.011);    // 1 INR = 0.011 EUR
        exchangeRates.put("GBP", 0.0095);   // 1 INR = 0.0095 GBP
    }

    public double convertFromINR(double amountInINR, String targetCurrency) {
        String currencyCode = targetCurrency.toUpperCase();
        
        // Default to INR if they ask for a currency we don't have
        if (!exchangeRates.containsKey(currencyCode)) {
            System.out.println("WARNING: Unsupported currency requested (" + currencyCode + "). Defaulting to INR.");
            return amountInINR; 
        }
        
        return amountInINR * exchangeRates.get(currencyCode);
    }
}