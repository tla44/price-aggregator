package com.ekorn.priceaggregator.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A data model representing a single price entry for a trading symbol.
 * <p>
 * Each {@code PriceEntry} contains:
 * <ul>
 *   <li>The trading symbol (e.g., "BTC-USD")</li>
 *   <li>The current price as a {@link BigDecimal}</li>
 *   <li>The timestamp in ISO 8601 string format indicating when the price was recorded</li>
 * </ul>
 * This class is used primarily for transferring price data across different application layers
 * and in REST API responses.
 */
public class PriceEntry {

  /**
   * The normalized trading symbol (e.g., "ETH-BTC").
   */
  private String symbol;

  /**
   * The latest price associated with the symbol.
   */
  private BigDecimal price;

  /**
   * The ISO 8601 string representation of the timestamp when the price was last updated.
   */
  private String timestamp;

  /**
   * Constructs a new {@code PriceEntry} with the specified symbol, price, and timestamp.
   *
   * @param symbol    the trading symbol
   * @param price     the current price
   * @param timestamp the time the price was recorded, stored internally as an ISO 8601 string
   */
  public PriceEntry(String symbol, BigDecimal price, Instant timestamp) {
    this.symbol = symbol;
    this.price = price;
    this.timestamp = timestamp.toString(); // ISO 8601
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

}

