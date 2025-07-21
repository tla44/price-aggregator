package com.ekorn.priceaggregator.verticle.adapter;

import com.ekorn.priceaggregator.model.PriceEntry;
import com.ekorn.priceaggregator.store.PriceStore;
import com.ekorn.priceaggregator.verticle.MarketVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A Vert.x verticle that connects to the Bitstamp WebSocket API to stream live trading data.
 * <p>
 * It filters and subscribes to a predefined set of trading pairs ("tracked symbols"),
 * processes incoming messages, and updates the shared {@link PriceStore}.
 */
public class BitStampWebSocketVerticle extends WebSocketVerticle {

  private static final Logger logger = LoggerFactory.getLogger(BitStampWebSocketVerticle.class);

  private static final String MARKETS_HOST = "https://www.bitstamp.net/api/v2/markets/";

  private static final String SOCKET_HOST = "ws.bitstamp.net";

  /**
   * A mapping of tracked trading symbols (e.g., BTC-USD) to their corresponding Bitstamp WebSocket channel names.
   */
  private final Map<String, String> channelToSymbol = new HashMap<>();

  /**
   * Constructs the BitStampWebSocketVerticle with a reference to a shared {@link PriceStore}.
   *
   */
  public BitStampWebSocketVerticle(String[] trackedSymbols) {
    super(trackedSymbols);
  }

  /**
   * Handles incoming WebSocket messages from Bitstamp.
   * <p>
   * Parses the message to extract the trading price, determines which symbol it maps to,
   * and updates the {@link PriceStore}.
   *
   * @param message the raw WebSocket message received as a string
   */
  public void handleMessage(String message) {
    JsonObject json = new JsonObject(message);
    JsonObject data = json.getJsonObject("data");
    if (data != null && data.containsKey("price")) {
      String matchedSymbol = channelToSymbol.get(json.getString("channel"));
      if (matchedSymbol != null) {
        PriceEntry priceEntry = new PriceEntry(matchedSymbol, new BigDecimal(data.getString("price")), Instant.now());
        vertx.eventBus().publish(MarketVerticle.EVENT_BUS_PRICE_UPDATE, JsonObject.mapFrom(priceEntry));
      }
    }
  }

  /**
   * Fetches available market symbols from Bitstamp and maps tracked symbols to WebSocket channels.
   *
   * @return a {@link Promise} that completes when symbol mapping is finished or fails if no relevant symbols are found
   */
  public Future<Void> fetchSymbols() {
    Promise<Void> promise = Promise.promise();
    WebClient client = WebClient.create(vertx);

    client.getAbs(MARKETS_HOST)
      .send()
      .onSuccess(response -> {
        JsonArray markets = response.bodyAsJsonArray();
        for (int i = 0; i < markets.size(); i++) {
          JsonObject market = markets.getJsonObject(i);
          String base = market.getString("base_currency");
          String counter = market.getString("counter_currency");
          String market_type = market.getString("market_type");
          String apiSymbol = market.getString("market_symbol");
          String normalizedSymbol = base + "-" + counter;
          for (String tracked : trackedSymbols) {
            if (tracked.equalsIgnoreCase(normalizedSymbol) && market_type.equalsIgnoreCase("SPOT")) {
              channelToSymbol.put("live_trades_" + apiSymbol, tracked);
            }
          }
        }
        if (channelToSymbol.isEmpty()) {
          promise.fail("No tracked symbols found in Bitstamp response.");
        } else {
          promise.complete();
        }
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  /**
   * Attempts to connect to Bitstamp's WebSocket API and subscribe to live trade channels.
   *
   * @return a {@link Future} representing the result of the connection attempt
   */
  public Future<WebSocket> attemptSocketConnection() {
    WebSocketClient client = vertx.createWebSocketClient();

    WebSocketConnectOptions connectOptions = new WebSocketConnectOptions()
      .setPort(443)
      .setHost(SOCKET_HOST)
      .setURI("/")
      .setSsl(true)
      .setVersion(WebsocketVersion.V13);

    return client.connect(connectOptions).onSuccess(ws -> {
      channelToSymbol.keySet().forEach(channel -> {
          JsonObject subscribeMessage = new JsonObject()
            .put("event", "bts:subscribe")
            .put("data", new JsonObject().put("channel", channel));
          logger.debug("Subscribed to {}", channel);
          ws.writeTextMessage(subscribeMessage.encode());
        });
    });
  }

  public Map<String, String> getChannelToSymbol() {
    return channelToSymbol;
  }

}
