package com.ekorn.priceaggregator.verticle.adapter;

import com.ekorn.priceaggregator.store.PriceStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base Vert.x verticle for WebSocket-based market data adapters.
 * <p>
 * This class handles common WebSocket connection logic, including automatic reconnects,
 * message handling delegation, and integration with a shared {@link PriceStore}.
 * <p>
 * Subclasses are responsible for providing:
 * <ul>
 *   <li>{@link #handleMessage(String)} – processing of incoming WebSocket messages</li>
 *   <li>{@link #fetchSymbols()} – retrieving and mapping symbols from the external service</li>
 *   <li>{@link #attemptSocketConnection()} – establishing the WebSocket connection</li>
 * </ul>
 */
public abstract class WebSocketVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketVerticle.class);

  /**
   * The set of market symbols to track. Subclasses should use this when filtering relevant market data.
   */
  protected final String[] trackedSymbols;

  /**
   * Delay before reconnecting in milliseconds if the WebSocket is unexpectedly closed.
   */
  private final long reconnectDelayMs = 10000;

  /**
   * Shared store used to persist or update price data across verticles.
   */
  protected final PriceStore store;

  /**
   * Variables for reconnection logic
   */
  private int reconnectAttempts = 0;
  private final long initialReconnectDelayMs = 2000;
  private final long maxReconnectDelayMs = 30000;

  /**
   * Constructs a new WebSocketVerticle with the given shared {@link PriceStore}.
   *
   * @param store the price store for persisting market data
   */
  public WebSocketVerticle(PriceStore store, String[] trackedSymbols) {
    this.trackedSymbols = trackedSymbols;
    this.store = store;
  }

  /**
   * Handles incoming WebSocket messages. Must be implemented by subclasses.
   *
   * @param message the message received as a string
   */
  abstract void handleMessage(String message);

  /**
   * Fetches and prepares symbol-channel mappings required for WebSocket subscriptions.
   *
   * @return a {@link Future} that completes when symbols are successfully fetched or fails if setup fails
   */
  abstract Future<Void> fetchSymbols();

  /**
   * Establishes a WebSocket connection to the data provider.
   * <p>
   * Subclasses should connect to their target endpoint and return the resulting WebSocket.
   *
   * @return a {@link Future} resolving to the connected {@link WebSocket}
   */
  abstract Future<WebSocket> attemptSocketConnection();

  /**
   * Starts the verticle by first fetching symbol mappings, then initiating a WebSocket connection.
   *
   * @param startPromise a promise that should be completed when the verticle is successfully started
   */
  @Override
  public void start(Promise<Void> startPromise) {
    fetchSymbols().onSuccess(v -> {
      initiateConnection();
      startPromise.complete();
    }).onFailure(err -> {
      logger.error("❌ Failed to fetch markets: {}", err.getMessage());
      startPromise.fail(err);
    });
  }

  /**
   * Initiates the WebSocket connection and registers message, close, and exception handlers.
   * Also stores the current active WebSocket reference.
   *
   */
  protected void initiateConnection() {
    attemptSocketConnection().onSuccess(ws -> {
      logger.info("✅ Connected to WebSocket");
      reconnectAttempts = 0;

      ws.textMessageHandler(this::handleMessage);

      long timerId = vertx.setPeriodic(10000, id -> {
        if (ws.isClosed()) {
          vertx.cancelTimer(id);
        } else {
          ws.writePing(Buffer.buffer());
          logger.debug("Ping sent");
        }
      });

      ws.closeHandler(v1 -> {
        logger.info("⚠️ WebSocket closed.");
        vertx.cancelTimer(timerId);
        scheduleReconnect(); // Trigger reconnect
      });

      ws.exceptionHandler(e -> {
        logger.error("WebSocket exception: {}", e.getMessage(), e);
        // The closeHandler will likely be called shortly after an exception, we can reconnect there
        if (!ws.isClosed()) {
          ws.close((short) 1011, "Exception occurred");
        }

      });

    }).onFailure(err -> {
      logger.error("❌ Connection attempt failed: {}", err.getMessage());
      scheduleReconnect(); // Trigger reconnect
    });
  }

  /**
   * Schedules a reconnect attempt with exponential backoff and a max cap.
   */
  protected void scheduleReconnect() {
    reconnectAttempts++;
    long delay = computeBackoffDelay();
    logger.info("🔁 Attempting to reconnect in {} ms (attempt #{})...", delay, reconnectAttempts);
    vertx.setTimer(delay, id -> initiateConnection());
  }

  /**
   * Computes the delay before the next reconnect attempt using exponential backoff.
   *
   * @return the computed delay in milliseconds
   */
  private long computeBackoffDelay() {
    return Math.min(initialReconnectDelayMs * (1L << (reconnectAttempts - 1)), maxReconnectDelayMs);
  }

}

