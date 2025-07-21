package com.ekorn.priceaggregator.verticle;

import com.ekorn.priceaggregator.model.PriceEntry;
import com.ekorn.priceaggregator.store.PriceStore;
import com.ekorn.priceaggregator.store.SimplePriceStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MarketVerticle is responsible for maintaining the state of market prices.
 * It listens to price update messages from exchange verticles and stores them.
 * It also handles queries from the REST API via the event bus.
 */
public class MarketVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MarketVerticle.class);

  // Address to which exchange adapters post new prices
  public static final String EVENT_BUS_PRICE_UPDATE = "market.price.update";

  // Address to which REST API verticle sends queries for prices
  public static final String EVENT_BUS_PRICE_QUERY = "market.price.query";

  private final PriceStore priceStore = new SimplePriceStore();

  @Override
  public void start() {
    // Listen for incoming price updates
    vertx.eventBus().consumer(EVENT_BUS_PRICE_UPDATE, this::handlePriceUpdate);

    // Listen for price queries
    vertx.eventBus().consumer(EVENT_BUS_PRICE_QUERY, this::handlePriceQuery);
  }

  private void handlePriceUpdate(Message<JsonObject> message) {
    try {
      PriceEntry priceEntry = message.body().mapTo(PriceEntry.class);
      priceStore.updatePrice(priceEntry);
      logger.debug("Stored price update for {}", priceEntry.getSymbol());
    } catch (Exception e) {
      logger.error("Failed to process price update", e);
    }
  }

  private void handlePriceQuery(Message<JsonObject> message) {
    String symbol = message.body().getString("symbol");
    if (symbol == null || symbol.isBlank()) {
      message.fail(400, "Missing or invalid 'symbol' field");
      return;
    }

    PriceEntry priceEntry = priceStore.getPrice(symbol);
    if (priceEntry != null) {
      message.reply(JsonObject.mapFrom(priceEntry));
    } else {
      message.fail(404, "Price not found for symbol: " + symbol);
    }
  }
}
