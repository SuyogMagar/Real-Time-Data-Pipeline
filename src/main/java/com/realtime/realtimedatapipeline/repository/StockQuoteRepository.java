package com.realtime.realtimedatapipeline.repository;

import com.realtime.realtimedatapipeline.model.StockQuoteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuoteEvent, UUID> {
    
    /**
     * Find all quotes for a specific symbol, ordered by timestamp descending
     */
    List<StockQuoteEvent> findBySymbolOrderByTimestampDesc(String symbol);
    
    /**
     * Find the most recent quote for a specific symbol
     */
    Optional<StockQuoteEvent> findFirstBySymbolOrderByTimestampDesc(String symbol);
    
    /**
     * Find all quotes for a symbol within a time range
     */
    @Query("SELECT s FROM StockQuoteEvent s WHERE s.symbol = :symbol AND s.timestamp BETWEEN :startTime AND :endTime ORDER BY s.timestamp DESC")
    List<StockQuoteEvent> findBySymbolAndTimestampBetween(@Param("symbol") String symbol, 
                                                         @Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find quotes for multiple symbols
     */
    @Query("SELECT s FROM StockQuoteEvent s WHERE s.symbol IN :symbols ORDER BY s.timestamp DESC")
    List<StockQuoteEvent> findBySymbolIn(@Param("symbols") List<String> symbols);
    
    /**
     * Find recent quotes (last N hours) for all symbols
     */
    @Query("SELECT s FROM StockQuoteEvent s WHERE s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<StockQuoteEvent> findRecentQuotes(@Param("since") LocalDateTime since);
    
    /**
     * Find quotes with significant price changes (above threshold percentage)
     */
    @Query("SELECT s FROM StockQuoteEvent s WHERE ABS(s.percentChange) >= :threshold ORDER BY s.timestamp DESC")
    List<StockQuoteEvent> findByPercentChangeGreaterThanEqual(@Param("threshold") Double threshold);
    
    /**
     * Get latest quote for each symbol (useful for current prices dashboard)
     */
    @Query("SELECT s1 FROM StockQuoteEvent s1 WHERE s1.timestamp = " +
           "(SELECT MAX(s2.timestamp) FROM StockQuoteEvent s2 WHERE s2.symbol = s1.symbol) " +
           "ORDER BY s1.symbol")
    List<StockQuoteEvent> findLatestQuoteForEachSymbol();
    
    /**
     * Count total quotes for a symbol
     */
    long countBySymbol(String symbol);
    
    /**
     * Find quotes with price above a certain value
     */
    @Query("SELECT s FROM StockQuoteEvent s WHERE s.currentPrice >= :minPrice ORDER BY s.currentPrice DESC")
    List<StockQuoteEvent> findByCurrentPriceGreaterThanEqual(@Param("minPrice") Double minPrice);
    
    /**
     * Get average price for a symbol over a time period
     */
    @Query("SELECT AVG(s.currentPrice) FROM StockQuoteEvent s WHERE s.symbol = :symbol AND s.timestamp BETWEEN :startTime AND :endTime")
    Optional<Double> getAveragePriceForSymbolInPeriod(@Param("symbol") String symbol, 
                                                      @Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get min and max prices for a symbol over a time period
     */
    @Query("SELECT MIN(s.currentPrice), MAX(s.currentPrice) FROM StockQuoteEvent s WHERE s.symbol = :symbol AND s.timestamp BETWEEN :startTime AND :endTime")
    Object[] getMinMaxPriceForSymbolInPeriod(@Param("symbol") String symbol, 
                                           @Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
}