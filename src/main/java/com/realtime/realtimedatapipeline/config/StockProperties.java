package com.realtime.realtimedatapipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.stocks")
public class StockProperties {
    
    private List<String> symbols = List.of("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN");
    private Duration updateInterval = Duration.ofSeconds(10);
    
    public List<String> getSymbols() {
        return symbols;
    }
    
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }
    
    public Duration getUpdateInterval() {
        return updateInterval;
    }
    
    public void setUpdateInterval(Duration updateInterval) {
        this.updateInterval = updateInterval;
    }
}