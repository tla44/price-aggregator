package com.ekorn.priceaggregator.config;

import java.util.Map;

/**
 * Represents the application configuration container.
 * <p>
 * This class holds configuration settings for multiple exchanges,
 * where each exchange is identified by a unique key (e.g., "bitstamp")
 * mapped to its corresponding {@link ExchangeConfig} instance.
 * <p>
 * The configuration is typically loaded from an external source (e.g., JSON file)
 * and used to provide runtime parameters for different exchange integrations.
 */
public class AppConfig {

  /**
   * Returns a map of exchange configurations.
   *
   * @return a map where keys are exchange names and values are their configuration objects
   */
  private Map<String, ExchangeConfig> exchanges;

  public Map<String, ExchangeConfig> getExchanges() {
    return exchanges;
  }

  public void setExchanges(Map<String, ExchangeConfig> exchanges) {
    this.exchanges = exchanges;
  }
}
