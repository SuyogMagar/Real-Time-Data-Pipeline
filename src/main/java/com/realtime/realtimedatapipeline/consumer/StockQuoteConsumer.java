package com.realtime.realtimedatapipeline.consumer;

import com.realtime.realtimedatapipeline.metrics.StockMetricsService;
import com.realtime.realtimedatapipeline.model.StockQuoteEvent;
import com.realtime.realtimedatapipeline.repository.StockQuoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StockQuoteConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(StockQuoteConsumer.class);
    
    private final StockQuoteRepository stockQuoteRepository;
    private final StockMetricsService metricsService;
    private final Counter consumedEventsCounter;
    private final Counter persistedEventsCounter;
    private final Counter errorCounter;
    
    public StockQuoteConsumer(StockQuoteRepository stockQuoteRepository, 
                             StockMetricsService metricsService, 
                             MeterRegistry meterRegistry) {
        this.stockQuoteRepository = stockQuoteRepository;
        this.metricsService = metricsService;
        this.consumedEventsCounter = Counter.builder("stock.events.consumed")
                .description("Number of stock quote events consumed from Kafka")
                .register(meterRegistry);
        this.persistedEventsCounter = Counter.builder("stock.events.persisted")
                .description("Number of stock quote events persisted to database")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("stock.consumer.errors")
                .description("Number of errors while consuming stock events")
                .register(meterRegistry);
    }
    
    @KafkaListener(topics = "stock-quotes", groupId = "stock-quote-consumer-group")
    public void consumeStockQuote(StockQuoteEvent stockQuoteEvent) {
        try {
            logger.info("Consuming stock quote event: {}", stockQuoteEvent);
            consumedEventsCounter.increment();
            
            // Create a new entity for persistence to avoid ID conflicts from Kafka deserialization
            StockQuoteEvent newEvent = StockQuoteEvent.builder()
                    .symbol(stockQuoteEvent.getSymbol())
                    .stockName(stockQuoteEvent.getStockName())
                    .currentPrice(stockQuoteEvent.getCurrentPrice())
                    .percentChange(stockQuoteEvent.getPercentChange())
                    .changeAmount(stockQuoteEvent.getChangeAmount())
                    .dayHigh(stockQuoteEvent.getDayHigh())
                    .dayLow(stockQuoteEvent.getDayLow())
                    .openPrice(stockQuoteEvent.getOpenPrice())
                    .previousClose(stockQuoteEvent.getPreviousClose())
                    .volume(stockQuoteEvent.getVolume())
                    .timestamp(stockQuoteEvent.getTimestamp())
                    .marketTimestamp(stockQuoteEvent.getMarketTimestamp())
                    .build();
            
            // Save to database
            StockQuoteEvent savedEvent = stockQuoteRepository.save(newEvent);
            persistedEventsCounter.increment();
            
            // Update metrics
            metricsService.updateStockMetrics(savedEvent);
            
            logger.info("Successfully persisted stock quote for symbol: {} with price: ${:.2f}", 
                       savedEvent.getSymbol(), savedEvent.getCurrentPrice());
                       
        } catch (Exception e) {
            logger.error("Error consuming and persisting stock quote event: {}", e.getMessage(), e);
            errorCounter.increment();
        }
    }
    
    @KafkaListener(topics = "stock-alerts", groupId = "stock-alert-consumer-group")
    public void consumeStockAlert(StockQuoteEvent alertEvent) {
        try {
            logger.info("Consuming stock alert event: {}", alertEvent);
            
            // Process alerts - could trigger notifications, store in separate table, etc.
            if (alertEvent.getPercentChange() != null && Math.abs(alertEvent.getPercentChange()) > 5.0) {
                logger.warn("SIGNIFICANT PRICE MOVEMENT ALERT: {} moved {:.2f}% to ${:.2f}", 
                           alertEvent.getSymbol(), alertEvent.getPercentChange(), alertEvent.getCurrentPrice());
                           
                // Record alert metrics
                metricsService.recordPriceAlert(alertEvent.getSymbol(), alertEvent.getPercentChange());
            }
            
        } catch (Exception e) {
            logger.error("Error processing stock alert: {}", e.getMessage(), e);
            errorCounter.increment();
        }
    }
}