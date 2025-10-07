package com.realtime.realtimedatapipeline.metrics;

import com.realtime.realtimedatapipeline.model.StockQuoteEvent;
import com.realtime.realtimedatapipeline.repository.StockQuoteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StockMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockMetricsService.class);
    
    private final StockQuoteRepository stockQuoteRepository;
    private final MeterRegistry meterRegistry;
    
    // Concurrent maps to store current stock prices and metrics
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> priceChanges = new ConcurrentHashMap<>();
    private final Map<String, Long> quoteCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastUpdateTimes = new ConcurrentHashMap<>();
    
    public StockMetricsService(StockQuoteRepository stockQuoteRepository, MeterRegistry meterRegistry) {
        this.stockQuoteRepository = stockQuoteRepository;
        this.meterRegistry = meterRegistry;
        registerCustomGauges();
    }
    
    /**
     * Register custom Prometheus gauges for stock metrics
     */
    private void registerCustomGauges() {
        // Simple metrics registration - no complex gauges for now
        logger.info("Stock metrics service initialized");
    }
    
    /**
     * Update metrics with new stock quote event
     */
    public void updateStockMetrics(StockQuoteEvent event) {
        if (event == null || event.getSymbol() == null) {
            return;
        }
        
        String symbol = event.getSymbol();
        
        // Update current price
        if (event.getCurrentPrice() != null) {
            currentPrices.put(symbol, event.getCurrentPrice());
        }
        
        // Update price change
        if (event.getPercentChange() != null) {
            priceChanges.put(symbol, event.getPercentChange());
        }
        
        // Update quote count
        quoteCounts.merge(symbol, 1L, Long::sum);
        
        // Update last update time
        lastUpdateTimes.put(symbol, LocalDateTime.now());
        
        logger.debug("Updated metrics for symbol: {} - Price: ${}, Change: {}%", 
                    symbol, event.getCurrentPrice(), event.getPercentChange());
    }
    
    /**
     * Scheduled method to refresh database-based metrics every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void refreshDatabaseMetrics() {
        try {
            logger.debug("Refreshing database-based stock metrics");
            
            // Get latest quotes for each symbol from database
            List<StockQuoteEvent> latestQuotes = stockQuoteRepository.findLatestQuoteForEachSymbol();
            
            for (StockQuoteEvent quote : latestQuotes) {
                updateStockMetrics(quote);
                
                // Track database freshness (no gauge registration needed)
                if (quote.getCreatedAt() != null) {
                    long minutesAgo = java.time.Duration.between(quote.getCreatedAt(), LocalDateTime.now()).toMinutes();
                    logger.debug("Data freshness for {}: {} minutes", quote.getSymbol(), minutesAgo);
                }
            }
            
            logger.debug("Refreshed metrics for {} symbols", latestQuotes.size());
            
        } catch (Exception e) {
            logger.error("Error refreshing database metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create metrics for significant price movements (alerts)
     */
    public void recordPriceAlert(String symbol, double priceChange) {
        Timer.Sample alertTimer = Timer.start(meterRegistry);
        alertTimer.stop(Timer.builder("stock.price.alert.processing.time")
                .description("Time to process price alert")
                .tag("symbol", symbol)
                .register(meterRegistry));
        
        // Count significant price movements
        meterRegistry.counter("stock.price.alerts.total",
                "symbol", symbol,
                "direction", priceChange > 0 ? "up" : "down")
                .increment();
    }
    
    
    
    /**
     * Get current stock metrics summary
     */
    public Map<String, Object> getMetricsSummary() {
        return Map.of(
            "trackedSymbols", currentPrices.keySet(),
            "currentPrices", currentPrices,
            "priceChanges", priceChanges,
            "quoteCounts", quoteCounts,
            "lastUpdates", lastUpdateTimes.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()
                    )),
            "totalDatabaseRecords", (double) stockQuoteRepository.count()
        );
    }
    
    /**
     * Get metrics for a specific symbol
     */
    public Map<String, Object> getSymbolMetrics(String symbol) {
        return Map.of(
            "symbol", symbol,
            "currentPrice", currentPrices.getOrDefault(symbol, 0.0),
            "priceChange", priceChanges.getOrDefault(symbol, 0.0),
            "quoteCount", quoteCounts.getOrDefault(symbol, 0L),
            "lastUpdate", lastUpdateTimes.getOrDefault(symbol, LocalDateTime.now()).toString()
        );
    }
}