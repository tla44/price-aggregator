package com.ekorn.priceaggregator.verticle;

import com.ekorn.priceaggregator.config.AppConfig;
import com.ekorn.priceaggregator.store.PriceStore;
import com.ekorn.priceaggregator.store.SimplePriceStore;
import com.ekorn.priceaggregator.verticle.adapter.BitStampWebSocketVerticle;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry Verticle for the Price Aggregator application.
 * <p>
 * This Verticle is responsible for bootstrapping the application by deploying the necessary
 * sub-verticles: one for WebSocket communication with BitStamp and one for the REST API.
 */
public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  /**
   * Called when the Verticle is deployed.
   * <p>
   * It initializes a shared {@link SimplePriceStore} and deploys the WebSocket Verticle(s) and REST API verticles.
   *
   * @param startPromise a promise to be completed when the verticle start-up is complete
   */
  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting price-aggregator...");
    PriceStore store = new SimplePriceStore();

    logger.info("Loading config...");
    Future<AppConfig> futureConfig = loadConfig();
    futureConfig.onSuccess(config -> {
      logger.info("Deploying verticles...");
      vertx.deployVerticle(() -> new BitStampWebSocketVerticle(store, config.getExchanges().get("bitstamp").getTrackedSymbols()), new DeploymentOptions());
      vertx.deployVerticle(() -> new RestApiVerticle(store), new DeploymentOptions())
        .onSuccess(id -> startPromise.complete())
        .onFailure(startPromise::fail);
      logger.info("Successfully started price-aggregator.");
    }).onFailure(startPromise::fail);

  }

  /**
   * Asynchronously loads the application configuration from a JSON file.
   * <p>
   * This method reads the {@code conf/config.json} file from the file system using Vert.x's
   * asynchronous file system API. The contents are parsed into a {@link JsonObject} and then
   * mapped to an {@link AppConfig} instance using Vert.x's built-in Jackson mapping.
   * <p>
   * If the file is missing, unreadable, or cannot be mapped to the {@code AppConfig} class,
   * the returned {@link Future} will fail with the corresponding exception.
   *
   * @return a {@link Future} that will be completed with the loaded {@link AppConfig} instance
   *         on success, or failed with an error if the configuration could not be loaded or parsed
   */
  private Future<AppConfig> loadConfig() {
    Promise<AppConfig> promise = Promise.promise();
    vertx.fileSystem().readFile("conf/config.json", result -> {
        if (result.failed()) {
          logger.error("Failed to load config", result.cause());
          promise.fail(result.cause());
        } else {
          JsonObject raw = result.result().toJsonObject();
          try {
            AppConfig config = raw.mapTo(AppConfig.class);
            promise.complete(config);
          } catch (IllegalArgumentException e) {
            logger.error("Unable to map config file", result.cause());
            promise.fail(e);
          }
        }
      });
    return promise.future();
  }

  /**
   * Optional main method for local debugging and development.
   * <p>
   * Deploys the {@code MainVerticle} manually using a standalone Vert.x instance.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle(), res -> {
      if (!res.succeeded()) {
        logger.error("Unable to start price-aggregator.");
        res.cause().printStackTrace();
      }
    });
  }

}
