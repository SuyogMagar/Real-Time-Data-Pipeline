package com.realtime.realtimedatapipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Finnhub stock quote API response
 * Maps to: https://finnhub.io/api/v1/quote?symbol=AAPL
 */
public class FinnhubQuoteResponse {
    
    @JsonProperty("c")
    private Double currentPrice;
    
    @JsonProperty("d")
    private Double change;
    
    @JsonProperty("dp")
    private Double percentChange;
    
    @JsonProperty("h")
    private Double highPrice;
    
    @JsonProperty("l")
    private Double lowPrice;
    
    @JsonProperty("o")
    private Double openPrice;
    
    @JsonProperty("pc")
    private Double previousClose;
    
    @JsonProperty("t")
    private Long timestamp;
    
    // Default constructor for JSON deserialization
    public FinnhubQuoteResponse() {}
    
    // Getters and setters
    public Double getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public Double getChange() {
        return change;
    }
    
    public void setChange(Double change) {
        this.change = change;
    }
    
    public Double getPercentChange() {
        return percentChange;
    }
    
    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }
    
    public Double getHighPrice() {
        return highPrice;
    }
    
    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }
    
    public Double getLowPrice() {
        return lowPrice;
    }
    
    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
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
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "FinnhubQuoteResponse{" +
                "currentPrice=" + currentPrice +
                ", change=" + change +
                ", percentChange=" + percentChange +
                ", highPrice=" + highPrice +
                ", lowPrice=" + lowPrice +
                ", openPrice=" + openPrice +
                ", previousClose=" + previousClose +
                ", timestamp=" + timestamp +
                '}';
    }
}