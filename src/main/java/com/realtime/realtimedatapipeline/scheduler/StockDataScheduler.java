package com.realtime.realtimedatapipeline.scheduler;

import com.realtime.realtimedatapipeline.client.FinnhubApiClient;
import com.realtime.realtimedatapipeline.config.StockProperties;
import com.realtime.realtimedatapipeline.model.FinnhubQuoteResponse;
import com.realtime.realtimedatapipeline.model.StockQuoteEvent;
import com.realtime.realtimedatapipeline.producer.StockDataProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled service to fetch stock data from Finnhub API and publish to Kafka
 */
@Service
public class StockDataScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(StockDataScheduler.class);
    
    private final FinnhubApiClient finnhubClient;
    private final StockDataProducer stockDataProducer;
    private final StockProperties stockProperties;
    
    private volatile boolean isRunning = false;
    private long fetchCount = 0;
    
    public StockDataScheduler(FinnhubApiClient finnhubClient, 
                             StockDataProducer stockDataProducer,
                             StockProperties stockProperties) {
        this.finnhubClient = finnhubClient;
        this.stockDataProducer = stockDataProducer;
        this.stockProperties = stockProperties;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Stock Data Scheduler initialized");
        logger.info("Tracking symbols: {}", stockProperties.getSymbols());
        logger.info("Update interval: {}", stockProperties.getUpdateInterval());
        
        // Fetch stock names on startup (cache them)
        CompletableFuture.runAsync(() -> {
            logger.info("Pre-loading stock company names...");
            stockProperties.getSymbols().forEach(symbol -> {
                String stockName = finnhubClient.getStockName(symbol);
                logger.debug("Cached company name for {}: {}", symbol, stockName);
            });
            logger.info("Stock company names pre-loaded");
        });
    }
    
    /**
     * Scheduled method to fetch stock data every 10 seconds (configurable)
     * Uses fixedRateString to read from configuration
     */
    @Scheduled(fixedRateString = "#{@stockProperties.getUpdateInterval().toMillis()}")
    @Async
    public void fetchAndPublishStockData() {
        if (isRunning) {
            logger.debug("Previous fetch still running, skipping this cycle");
            return;
        }
        
        try {
            isRunning = true;
            fetchCount++;
            
            List<String> symbols = stockProperties.getSymbols();
            logger.info("Starting stock data fetch cycle #{} for {} symbols: {}", 
                    fetchCount, symbols.size(), symbols);
            
            // Fetch data for all symbols in parallel
            List<CompletableFuture<Void>> futures = symbols.stream()
                    .map(this::fetchAndPublishSingleStock)
                    .toList();
            
            // Wait for all fetches to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();
            
            logger.info("Completed stock data fetch cycle #{}", fetchCount);
            
        } catch (Exception e) {
            logger.error("Error in scheduled stock data fetch: {}", e.getMessage(), e);
        } finally {
            isRunning = false;
        }
    }
    
    /**
     * Fetch and publish data for a single stock symbol
     * @param symbol Stock symbol to fetch
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> fetchAndPublishSingleStock(String symbol) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Fetching data for symbol: {}", symbol);
                
                // Fetch quote from Finnhub
                FinnhubQuoteResponse quote = finnhubClient.getStockQuote(symbol);
                
                if (quote == null || quote.getCurrentPrice() == null) {
                    logger.warn("No data received for symbol: {}", symbol);
                    return;
                }
                
                // Get cached stock name
                String stockName = finnhubClient.getStockName(symbol);
                
                // Convert to domain event
                StockQuoteEvent event = convertToStockQuoteEvent(symbol, stockName, quote);
                
                // Publish to Kafka
                stockDataProducer.publishStockQuote(event);
                
                logger.debug("Successfully processed quote for {}: ${} ({}%)", 
                        symbol, quote.getCurrentPrice(), quote.getPercentChange());
                
            } catch (Exception e) {
                logger.error("Error processing stock data for symbol {}: {}", symbol, e.getMessage());
            }
        });
    }
    
    /**
     * Convert Finnhub response to domain event
     */
    private StockQuoteEvent convertToStockQuoteEvent(String symbol, String stockName, FinnhubQuoteResponse quote) {
        LocalDateTime timestamp = quote.getTimestamp() != null 
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(quote.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();
        
        return StockQuoteEvent.builder()
                .symbol(symbol)
                .stockName(stockName)
                .currentPrice(quote.getCurrentPrice())
                .percentChange(quote.getPercentChange())
                .changeAmount(quote.getChange())
                .dayHigh(quote.getHighPrice())
                .dayLow(quote.getLowPrice())
                .openPrice(quote.getOpenPrice())
                .previousClose(quote.getPreviousClose())
                .timestamp(timestamp)
                .marketTimestamp(quote.getTimestamp())
                .build();
    }
    
    /**
     * Manual trigger for testing purposes
     * @return Number of symbols processed
     */
    public int triggerManualFetch() {
        logger.info("Manual stock data fetch triggered");
        
        List<String> symbols = stockProperties.getSymbols();
        symbols.forEach(symbol -> {
            try {
                fetchAndPublishSingleStock(symbol).get();
            } catch (Exception e) {
                logger.error("Error in manual fetch for symbol {}: {}", symbol, e.getMessage());
            }
        });
        
        return symbols.size();
    }
    
    /**
     * Get scheduler statistics
     */
    public java.util.Map<String, Object> getStatistics() {
        return java.util.Map.of(
            "fetchCount", fetchCount,
            "isRunning", isRunning,
            "trackedSymbols", stockProperties.getSymbols().size(),
            "updateInterval", stockProperties.getUpdateInterval().toString()
        );
    }
    
    /**
     * Check if scheduler is healthy
     */
    public boolean isHealthy() {
        return finnhubClient.isHealthy();
    }
}
