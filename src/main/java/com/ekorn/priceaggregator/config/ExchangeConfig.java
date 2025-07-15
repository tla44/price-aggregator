package com.ekorn.priceaggregator.config;

/**
 * Represents configuration settings for a single exchange.
 * <p>
 * This class holds specific parameters related to an exchange,
 * such as the list of market symbols to track.
 */
public class ExchangeConfig {

  /**
   * Returns the list of symbols that should be tracked for this exchange.
   *
   * @return an array of symbol strings (e.g., "BTC-USD", "ETH-USD")
   */
  private String[] trackedSymbols;

  public String[] getTrackedSymbols() {
    return trackedSymbols;
  }

  public void setTrackedSymbols(String[] trackedSymbols) {
    this.trackedSymbols = trackedSymbols;
  }
}
