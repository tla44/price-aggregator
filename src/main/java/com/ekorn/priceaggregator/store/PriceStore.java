package com.ekorn.priceaggregator.store;

import com.ekorn.priceaggregator.model.PriceEntry;

import java.math.BigDecimal;
import java.time.Instant;

public interface PriceStore {

  /**
   * Updates the price of a tracked symbol with a new value and timestamp.
   * If the symbol doesn't exist yet, it is added.
   *
   * @param symbol    the trading symbol (e.g., "ETH-USD")
   * @param price     the latest price to store
   * @param timestamp the timestamp when the price was received or computed
   */
  void updatePrice(String symbol, BigDecimal price, Instant timestamp);

  /**
   * Retrieves the most recently stored price for the given symbol.
   *
   * @param symbol the trading symbol to query
   * @return the {@link PriceEntry} associated with the symbol, or {@code null} if none exists
   */
  PriceEntry getPrice(String symbol);

}
