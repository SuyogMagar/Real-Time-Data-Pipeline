package com.realtime.realtimedatapipeline.controller;

import com.realtime.realtimedatapipeline.client.FinnhubApiClient;
import com.realtime.realtimedatapipeline.config.StockProperties;
import com.realtime.realtimedatapipeline.producer.StockDataProducer;
import com.realtime.realtimedatapipeline.scheduler.StockDataScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for testing and monitoring the stock data pipeline
 */
@RestController
@RequestMapping("/api/stocks")
public class StockDataController {
    
    private final StockDataScheduler stockDataScheduler;
    private final StockDataProducer stockDataProducer;
    private final FinnhubApiClient finnhubApiClient;
    private final StockProperties stockProperties;
    
    public StockDataController(StockDataScheduler stockDataScheduler,
                              StockDataProducer stockDataProducer,
                              FinnhubApiClient finnhubApiClient,
                              StockProperties stockProperties) {
        this.stockDataScheduler = stockDataScheduler;
        this.stockDataProducer = stockDataProducer;
        this.finnhubApiClient = finnhubApiClient;
        this.stockProperties = stockProperties;
    }
    
    /**
     * Get current tracked symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<Map<String, Object>> getTrackedSymbols() {
        return ResponseEntity.ok(Map.of(
            "symbols", stockProperties.getSymbols(),
            "updateInterval", stockProperties.getUpdateInterval().toString(),
            "totalSymbols", stockProperties.getSymbols().size()
        ));
    }
    
    /**
     * Manually trigger stock data fetch
     */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> triggerFetch() {
        int processed = stockDataScheduler.triggerManualFetch();
        return ResponseEntity.ok(Map.of(
            "message", "Stock data fetch triggered",
            "symbolsProcessed", processed
        ));
    }
    
    /**
     * Get scheduler statistics
     */
    @GetMapping("/stats/scheduler")
    public ResponseEntity<Map<String, Object>> getSchedulerStats() {
        return ResponseEntity.ok(stockDataScheduler.getStatistics());
    }
    
    /**
     * Get producer statistics
     */
    @GetMapping("/stats/producer")
    public ResponseEntity<Map<String, Object>> getProducerStats() {
        return ResponseEntity.ok(stockDataProducer.getStatistics());
    }
    
    /**
     * Get Finnhub API statistics
     */
    @GetMapping("/stats/api")
    public ResponseEntity<Map<String, Object>> getApiStats() {
        return ResponseEntity.ok(finnhubApiClient.getStatistics());
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = stockDataScheduler.isHealthy();
        return ResponseEntity.ok(Map.of(
            "healthy", isHealthy,
            "finnhubApiHealthy", finnhubApiClient.isHealthy(),
            "schedulerRunning", !stockDataScheduler.getStatistics().get("isRunning").equals(false)
        ));
    }
    
    /**
     * Get overall pipeline status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPipelineStatus() {
        return ResponseEntity.ok(Map.of(
            "scheduler", stockDataScheduler.getStatistics(),
            "producer", stockDataProducer.getStatistics(),
            "api", finnhubApiClient.getStatistics(),
            "configuration", Map.of(
                "symbols", stockProperties.getSymbols(),
                "updateInterval", stockProperties.getUpdateInterval().toString()
            )
        ));
    }
}
