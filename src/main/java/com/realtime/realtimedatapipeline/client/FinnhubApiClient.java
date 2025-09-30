package com.realtime.realtimedatapipeline.client;

import com.realtime.realtimedatapipeline.config.FinnhubProperties;
import com.realtime.realtimedatapipeline.model.FinnhubQuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST client for Finnhub API
 * Handles rate limiting, retries, and metrics collection
 */
@Component
public class FinnhubApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FinnhubApiClient.class);
    
    private final WebClient webClient;
    private final FinnhubProperties finnhubProperties;
    
    // Simple counters
    private long requestCount = 0;
    private long errorCount = 0;
    
    // Cache for stock company profiles (to get company names)
    private final Map<String, String> stockNameCache = new ConcurrentHashMap<>();
    
    public FinnhubApiClient(FinnhubProperties finnhubProperties) {
        this.finnhubProperties = finnhubProperties;
        
        // Initialize WebClient with base configuration
        this.webClient = WebClient.builder()
                .baseUrl(finnhubProperties.getBaseUrl())
                .build();
        
        logger.info("Finnhub API Client initialized with base URL: {}", finnhubProperties.getBaseUrl());
    }
    
    /**
     * Fetch stock quote for a given symbol
     * @param symbol Stock symbol (e.g., "AAPL")
     * @return FinnhubQuoteResponse or null if error
     */
    public FinnhubQuoteResponse getStockQuote(String symbol) {
        try {
            requestCount++;
            
            logger.debug("Fetching quote for symbol: {}", symbol);
            
            FinnhubQuoteResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", finnhubProperties.getApiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(FinnhubQuoteResponse.class)
                    .timeout(finnhubProperties.getTimeout())
                    .block();
            
            if (response != null && response.getCurrentPrice() != null) {
                logger.debug("Successfully fetched quote for {}: ${}", symbol, response.getCurrentPrice());
                return response;
            } else {
                logger.warn("Received empty or invalid response for symbol: {}", symbol);
                errorCount++;
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error fetching stock quote for symbol {}: {}", symbol, e.getMessage());
            errorCount++;
            return null;
        }
    }
    
    /**
     * Get company name for a stock symbol (cached)
     * @param symbol Stock symbol
     * @return Company name or symbol if not found
     */
    public String getStockName(String symbol) {
        return stockNameCache.computeIfAbsent(symbol, this::fetchStockName);
    }
    
    /**
     * Fetch company profile from Finnhub API
     * @param symbol Stock symbol
     * @return Company name or symbol as fallback
     */
    private String fetchStockName(String symbol) {
        try {
            logger.debug("Fetching company profile for symbol: {}", symbol);
            
            Map<String, Object> profile = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/profile2")
                            .queryParam("symbol", symbol)
                            .queryParam("token", finnhubProperties.getApiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(finnhubProperties.getTimeout())
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block();
            
            if (profile != null && profile.containsKey("name")) {
                String companyName = (String) profile.get("name");
                logger.debug("Found company name for {}: {}", symbol, companyName);
                return companyName;
            }
            
        } catch (Exception e) {
            logger.warn("Could not fetch company name for symbol {}: {}", symbol, e.getMessage());
        }
        
        // Fallback to symbol if company name not found
        return symbol;
    }
    
    /**
     * Check if API client is healthy
     * @return true if API is accessible
     */
    public boolean isHealthy() {
        try {
            // Try to fetch a quote for a reliable stock (AAPL)
            FinnhubQuoteResponse response = getStockQuote("AAPL");
            return response != null && response.getCurrentPrice() != null;
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current request statistics
     * @return Map with request metrics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "totalRequests", requestCount,
            "totalErrors", errorCount,
            "successRate", calculateSuccessRate()
        );
    }
    
    private double calculateSuccessRate() {
        if (requestCount == 0) return 0.0;
        return ((requestCount - errorCount) / (double) requestCount) * 100.0;
    }
}
