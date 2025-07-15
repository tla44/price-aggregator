package com.ekorn.priceaggregator.verticle;

import com.ekorn.priceaggregator.store.PriceStore;
import com.ekorn.priceaggregator.model.PriceEntry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Vert.x verticle that exposes a simple HTTP REST API for querying live price data.
 * <p>
 * This verticle serves as the HTTP interface of the application, providing a public endpoint
 * to fetch the most recent market price for a given trading symbol.
 *
 * <p><b>Exposed Endpoint:</b>
 * <ul>
 *   <li><code>GET /prices/:symbol</code> – Returns the most recent {@link PriceEntry} for a given symbol</li>
 * </ul>
 *
 * The symbol is expected to be passed in uppercase (e.g., <code>BTC-USD</code>).
 */
public class RestApiVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(RestApiVerticle.class);

  /**
   * The shared store that provides access to up-to-date market prices.
   */
  private final PriceStore store;

  /**
   * Constructs a new REST API verticle with the given {@link PriceStore}.
   *
   * @param store the store used to retrieve the latest price data
   */
  public RestApiVerticle(PriceStore store) {
    this.store = store;
  }

  /**
   * Starts the HTTP server and sets up routing for REST API endpoints.
   * <p>
   * The server listens on port <b>8888</b> and exposes a single route to fetch the latest price.
   *
   * @param startPromise the promise to be completed when the HTTP server is successfully started
   */
  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);

    router.get("/prices/:symbol").handler(ctx -> {
      String symbol = ctx.pathParam("symbol").toUpperCase();
      PriceEntry entry = store.getPrice(symbol);
      if (entry == null) {
        ctx.response().setStatusCode(404)
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject().put("error", "Symbol not available").encode());
      } else {
        ctx.response()
          .putHeader("Content-Type", "application/json")
          .end(new JsonObject()
            .put("symbol", entry.getSymbol())
            .put("price", entry.getPrice().toPlainString())
            .put("timestamp", entry.getTimestamp()).encode()
          );
      }
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888) // TODO - Add port to config
      .onSuccess(server -> startPromise.complete())
      .onFailure(startPromise::fail);
  }
}
