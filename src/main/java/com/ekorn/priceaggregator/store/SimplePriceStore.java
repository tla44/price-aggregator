package com.ekorn.priceaggregator.store;

import com.ekorn.priceaggregator.model.PriceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe in-memory store for holding the latest price entries of tracked trading symbols.
 * <p>
 * Used by WebSocket and REST verticles to store and retrieve up-to-date market data.
 */
public class SimplePriceStore implements PriceStore {

  private static final Logger logger = LoggerFactory.getLogger(SimplePriceStore.class);

  /**
   * A concurrent map storing the latest price for each symbol.
   * Keys are normalized trading symbols (e.g., "BTC-USD").
   */
  private final Map<String, PriceEntry> priceMap = new ConcurrentHashMap<>();

  /**
   * Updates the price of a tracked symbol with a new value and timestamp.
   * If the symbol doesn't exist yet, it is added.
   *
   * @param symbol    the trading symbol (e.g., "ETH-USD")
   * @param price     the latest price to store
   * @param timestamp the timestamp when the price was received or computed
   */
  public void updatePrice(String symbol, BigDecimal price, Instant timestamp) {
    updatePrice(new PriceEntry(symbol, price, timestamp));
  }

  public void updatePrice(PriceEntry priceEntry) {
    if (null != priceEntry && null != priceEntry.getSymbol()
      && !priceEntry.getSymbol().isEmpty()
      && null != priceEntry.getPrice()
      && null != priceEntry.getTimestamp()) {
      logger.debug("Updated price for {}. New price {}. Timestamp {}", priceEntry.getSymbol(), priceEntry.getPrice(), priceEntry.getTimestamp());
      priceMap.put(priceEntry.getSymbol(), priceEntry);
    }
  }

  /**
   * Retrieves the most recently stored price for the given symbol.
   *
   * @param symbol the trading symbol to query
   * @return the {@link PriceEntry} associated with the symbol, or {@code null} if none exists
   */
  public PriceEntry getPrice(String symbol) {
    if (null != symbol) {
      return priceMap.get(symbol);
    }
    return null;
  }
}
