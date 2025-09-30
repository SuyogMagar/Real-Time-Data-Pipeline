package com.realtime.realtimedatapipeline.model;

import com.fasterxml.jackson.annotation.JsonFormat;
// import jakarta.persistence.*;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for stock quote events
 * Used for Kafka messaging and database persistence
 */
// @Entity
// @Table(name = "stock_quotes", schema = "events")
// @NamedQuery(
//     name = "StockQuoteEvent.findRecentBySymbol",
//     query = "SELECT s FROM StockQuoteEvent s WHERE s.symbol = :symbol ORDER BY s.timestamp DESC"
// )
public class StockQuoteEvent {
    
    // @Id
    // @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    // @NotBlank
    // @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;
    
    // @NotBlank
    // @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;
    
    // @NotNull
    // @Column(name = "current_price", nullable = false)
    private Double currentPrice;
    private Double percentChange;
    private Double changeAmount;
    private Double dayHigh;
    private Double dayLow;
    private Double openPrice;
    private Double previousClose;
    private Long volume;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Long marketTimestamp;
    private LocalDateTime createdAt;
    
    // Constructors
    public StockQuoteEvent() {
        this.createdAt = LocalDateTime.now();
    }
    
    public StockQuoteEvent(String symbol, String stockName, Double currentPrice, 
                          Double percentChange, LocalDateTime timestamp) {
        this();
        this.symbol = symbol;
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.percentChange = percentChange;
        this.timestamp = timestamp;
    }
    
    // Builder pattern for easy object creation
    public static StockQuoteEventBuilder builder() {
        return new StockQuoteEventBuilder();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getStockName() {
        return stockName;
    }
    
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }
    
    public Double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public Double getPercentChange() {
        return percentChange;
    }
    
    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }
    
    public Double getChangeAmount() {
        return changeAmount;
    }
    
    public void setChangeAmount(Double changeAmount) {
        this.changeAmount = changeAmount;
    }
    
    public Double getDayHigh() {
        return dayHigh;
    }
    
    public void setDayHigh(Double dayHigh) {
        this.dayHigh = dayHigh;
    }
    
    public Double getDayLow() {
        return dayLow;
    }
    
    public void setDayLow(Double dayLow) {
        this.dayLow = dayLow;
    }
    
    public Double getOpenPrice() {
        return openPrice;
    }
    
    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
    }
    
    public Double getPreviousClose() {
        return previousClose;
    }
    
    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }
    
    public Long getVolume() {
        return volume;
    }
    
    public void setVolume(Long volume) {
        this.volume = volume;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Long getMarketTimestamp() {
        return marketTimestamp;
    }
    
    public void setMarketTimestamp(Long marketTimestamp) {
        this.marketTimestamp = marketTimestamp;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return String.format("StockQuoteEvent{symbol='%s', stockName='%s', currentPrice=%.2f, percentChange=%.2f%%, timestamp=%s}",
                symbol, stockName, currentPrice, percentChange, timestamp);
    }
    
    // Builder class
    public static class StockQuoteEventBuilder {
        private StockQuoteEvent event = new StockQuoteEvent();
        
        public StockQuoteEventBuilder symbol(String symbol) {
            event.setSymbol(symbol);
            return this;
        }
        
        public StockQuoteEventBuilder stockName(String stockName) {
            event.setStockName(stockName);
            return this;
        }
        
        public StockQuoteEventBuilder currentPrice(Double currentPrice) {
            event.setCurrentPrice(currentPrice);
            return this;
        }
        
        public StockQuoteEventBuilder percentChange(Double percentChange) {
            event.setPercentChange(percentChange);
            return this;
        }
        
        public StockQuoteEventBuilder changeAmount(Double changeAmount) {
            event.setChangeAmount(changeAmount);
            return this;
        }
        
        public StockQuoteEventBuilder dayHigh(Double dayHigh) {
            event.setDayHigh(dayHigh);
            return this;
        }
        
        public StockQuoteEventBuilder dayLow(Double dayLow) {
            event.setDayLow(dayLow);
            return this;
        }
        
        public StockQuoteEventBuilder openPrice(Double openPrice) {
            event.setOpenPrice(openPrice);
            return this;
        }
        
        public StockQuoteEventBuilder previousClose(Double previousClose) {
            event.setPreviousClose(previousClose);
            return this;
        }
        
        public StockQuoteEventBuilder volume(Long volume) {
            event.setVolume(volume);
            return this;
        }
        
        public StockQuoteEventBuilder timestamp(LocalDateTime timestamp) {
            event.setTimestamp(timestamp);
            return this;
        }
        
        public StockQuoteEventBuilder marketTimestamp(Long marketTimestamp) {
            event.setMarketTimestamp(marketTimestamp);
            return this;
        }
        
        public StockQuoteEvent build() {
            return event;
        }
    }
}