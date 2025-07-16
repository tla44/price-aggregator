package com.ekorn.priceaggregator.verticle;

import com.ekorn.priceaggregator.model.PriceEntry;
import com.ekorn.priceaggregator.store.PriceStore;
import io.vertx.core.Vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the RestApiVerticle.
 * These tests deploy the verticle and make actual HTTP requests to verify its behavior.
 */
@ExtendWith(VertxExtension.class) // Enables Vert.x JUnit 5 extension
class RestApiVerticleTest {

  private Vertx vertx;

  @Mock
  private PriceStore mockPriceStore; // Mock the PriceStore dependency

  private WebClient webClient; // Vert.x HTTP client for making requests

  /**
   * Sets up the test environment before each test.
   * Initializes Vert.x, mocks, and deploys the verticle.
   *
   * @param testContext Vert.x test context for asynchronous operations
   */
  @BeforeEach
  void setUp(VertxTestContext testContext) {
    // Initialize Mockito mocks
    MockitoAnnotations.openMocks(this);

    // Create a new Vert.x instance for each test
    vertx = Vertx.vertx();

    // Create a WebClient instance for making HTTP requests
    webClient = WebClient.create(vertx);

    // Deploy the RestApiVerticle, injecting the mocked PriceStore
    vertx.deployVerticle(() -> new RestApiVerticle(mockPriceStore), new DeploymentOptions())
      .onComplete(testContext.succeedingThenComplete()); // Signal test context that deployment is complete
  }

  /**
   * Cleans up the test environment after each test.
   * Closes the Vert.x instance to release resources, including the bound port.
   *
   * @param testContext Vert.x test context for asynchronous operations
   */
  @AfterEach
  void tearDown(VertxTestContext testContext) {
    // Close the Vert.x instance. This will also close any deployed verticles and HTTP servers.
    vertx.close()
      .onComplete(testContext.succeedingThenComplete()); // Signal test context that cleanup is complete
  }

  /**
   * Test case for successfully retrieving a price for an existing symbol.
   *
   * @param testContext Vert.x test context
   */
  @Test
  @DisplayName("Should return 200 OK and price entry for existing symbol")
  void getPrice_existingSymbol_shouldReturnOkAndPriceEntry(VertxTestContext testContext) {
    String symbol = "BTC-USD";
    BigDecimal price = new BigDecimal("60000.00");
    Instant timestamp = Instant.now();
    PriceEntry expectedEntry = new PriceEntry(symbol, price, timestamp);

    // Configure the mock PriceStore to return the expected entry when getPrice is called
    when(mockPriceStore.getPrice(symbol.toUpperCase())).thenReturn(expectedEntry);

    // Make an HTTP GET request to the verticle's endpoint
    webClient.get(8888, "localhost", "/prices/" + symbol)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Assertions on the HTTP response
        assertEquals(200, response.statusCode(), "Status code should be 200 OK");
        assertEquals("application/json", response.headers().get("Content-Type"), "Content-Type should be application/json");

        // Parse the response body and assert its content
        JsonObject responseBody = response.bodyAsJsonObject();
        assertNotNull(responseBody, "Response body should not be null");
        assertEquals(symbol, responseBody.getString("symbol"), "Symbol in response should match");
        assertEquals(price, new BigDecimal(responseBody.getString("price")), "Price in response should match");
        assertEquals(timestamp.toString(), responseBody.getString("timestamp"), "Timestamp in response should match");

        // Verify that the mock PriceStore's getPrice method was called with the correct argument
        verify(mockPriceStore, times(1)).getPrice(symbol.toUpperCase());
        testContext.completeNow(); // Signal test context that this test case is complete
      })));
  }

  /**
   * Test case for retrieving a price for a non-existent symbol.
   *
   * @param testContext Vert.x test context
   */
  @Test
  @DisplayName("Should return 404 Not Found for non-existent symbol")
  void getPrice_nonExistentSymbol_shouldReturnNotFound(VertxTestContext testContext) {
    String symbol = "NONEXISTENT-SYM";

    // Configure the mock PriceStore to return null when getPrice is called for this symbol
    when(mockPriceStore.getPrice(symbol.toUpperCase())).thenReturn(null);

    // Make an HTTP GET request
    webClient.get(8888, "localhost", "/prices/" + symbol)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        // Assertions on the HTTP response
        assertEquals(404, response.statusCode(), "Status code should be 404 Not Found");
        assertEquals("application/json", response.headers().get("Content-Type"), "Content-Type should be application/json");

        // Parse the response body and assert its content
        JsonObject responseBody = response.bodyAsJsonObject();
        assertNotNull(responseBody, "Response body should not be null");
        assertEquals("Symbol not available", responseBody.getString("error"), "Error message should match");

        // Verify that the mock PriceStore's getPrice method was called
        verify(mockPriceStore, times(1)).getPrice(symbol.toUpperCase());
        testContext.completeNow();
      })));
  }

  /**
   * Test case to ensure symbol is converted to uppercase before querying the store.
   *
   * @param testContext Vert.x test context
   */
  @Test
  @DisplayName("Should convert symbol to uppercase before querying PriceStore")
  void getPrice_lowercaseSymbol_shouldQueryStoreWithUppercase(VertxTestContext testContext) {
    String symbol = "eth-usd"; // Lowercase symbol
    BigDecimal price = new BigDecimal("3500.00");
    Instant timestamp = Instant.now();
    PriceEntry expectedEntry = new PriceEntry(symbol.toUpperCase(), price, timestamp);

    // Configure the mock PriceStore to return an entry when queried with the uppercase symbol
    when(mockPriceStore.getPrice(symbol.toUpperCase())).thenReturn(expectedEntry);

    // Make an HTTP GET request with the lowercase symbol
    webClient.get(8888, "localhost", "/prices/" + symbol)
      .send()
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        // Verify that getPrice was called with the uppercase version of the symbol
        verify(mockPriceStore, times(1)).getPrice("ETH-USD");
        testContext.completeNow();
      })));
  }
}
