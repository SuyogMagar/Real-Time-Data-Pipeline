package com.realtime.realtimedatapipeline.producer;

import com.realtime.realtimedatapipeline.model.StockQuoteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for producing stock quote events to Kafka topics
 */
@Service
public class StockDataProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(StockDataProducer.class);
    
    private final KafkaTemplate<String, StockQuoteEvent> kafkaTemplate;
    
    @Value("${app.kafka.topics.stock-quotes-raw}")
    private String stockQuotesRawTopic;
    
    // Simple counters
    private long publishedEvents = 0;
    private long failedEvents = 0;
    
    public StockDataProducer(KafkaTemplate<String, StockQuoteEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish stock quote event to raw topic
     * @param stockQuoteEvent The stock quote event to publish
     */
    public void publishStockQuote(StockQuoteEvent stockQuoteEvent) {
        try {
            // Use symbol as partition key for consistent partitioning
            String partitionKey = stockQuoteEvent.getSymbol();
            
            logger.info("Publishing stock quote for symbol: {} - Price: ${}", 
                    stockQuoteEvent.getSymbol(), stockQuoteEvent.getCurrentPrice());
            
            kafkaTemplate.send(stockQuotesRawTopic, partitionKey, stockQuoteEvent);
            publishedEvents++;
            
        } catch (Exception e) {
            failedEvents++;
            logger.error("Error publishing stock quote event for symbol: {}", 
                    stockQuoteEvent.getSymbol(), e);
        }
    }
    
    
    /**
     * Get producer statistics
     * @return Map with producer metrics
     */
    public java.util.Map<String, Object> getStatistics() {
        return java.util.Map.of(
            "publishedEvents", publishedEvents,
            "failedEvents", failedEvents,
            "successRate", calculateSuccessRate()
        );
    }
    
    private double calculateSuccessRate() {
        double total = publishedEvents + failedEvents;
        if (total == 0) return 0.0;
        return (publishedEvents / total) * 100.0;
    }
}