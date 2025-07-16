package com.ekorn.priceaggregator.store;

import com.ekorn.priceaggregator.model.PriceEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SimplePriceStore class.
 * These tests ensure that the store correctly updates and retrieves price entries.
 */
class SimplePriceStoreTest {

  @InjectMocks
  private SimplePriceStore simplePriceStore;

  /**
   * Set up method to initialize Mockito mocks before each test.
   */
  @BeforeEach
  void setUp() {
    // Initialize mocks.
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Test case for updating a price for a new symbol.
   * Verifies that a new PriceEntry is correctly stored and can be retrieved.
   */
  @Test
  void updatePrice_newSymbol_shouldStorePriceEntry() {
    String symbol = "BTC-USD";
    BigDecimal price = new BigDecimal("50000.00");
    Instant timestamp = Instant.now();

    // Call the method under test
    simplePriceStore.updatePrice(symbol, price, timestamp);

    // Retrieve the price and assert its properties
    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);

    assertNotNull(retrievedEntry, "PriceEntry should not be null for a new symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match the updated symbol.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match the updated price.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match the updated timestamp.");
  }

  /**
   * Test case for updating a price for an existing symbol.
   * Verifies that the existing PriceEntry is overwritten with the new values.
   */
  @Test
  void updatePrice_existingSymbol_shouldUpdatePriceEntry() {
    String symbol = "ETH-USD";
    BigDecimal initialPrice = new BigDecimal("3000.00");
    Instant initialTimestamp = Instant.now().minusSeconds(3600); // 1 hour ago

    // Store an initial price
    simplePriceStore.updatePrice(symbol, initialPrice, initialTimestamp);

    // New values for update
    BigDecimal newPrice = new BigDecimal("3100.50");
    Instant newTimestamp = Instant.now();

    // Call the method under test to update the existing symbol
    simplePriceStore.updatePrice(symbol, newPrice, newTimestamp);

    // Retrieve the price and assert its updated properties
    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);

    assertNotNull(retrievedEntry, "PriceEntry should not be null after update.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should remain the same.");
    assertEquals(newPrice, retrievedEntry.getPrice(), "Price should be updated to the new price.");
    assertEquals(newTimestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should be updated to the new timestamp.");

  }

  /**
   * Test case for retrieving a price for an existing symbol.
   * Verifies that the correct PriceEntry is returned.
   */
  @Test
  void getPrice_existingSymbol_shouldReturnPriceEntry() {
    String symbol = "XRP-USD";
    BigDecimal price = new BigDecimal("0.75");
    Instant timestamp = Instant.now();

    // Store a price entry first
    simplePriceStore.updatePrice(symbol, price, timestamp);

    // Retrieve the price
    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);

    assertNotNull(retrievedEntry, "PriceEntry should not be null for an existing symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match.");
  }

  /**
   * Test case for retrieving a price for a non-existent symbol.
   * Verifies that null is returned.
   */
  @Test
  void getPrice_nonExistentSymbol_shouldReturnNull() {
    String symbol = "LTC-USD"; // This symbol has not been added

    // Attempt to retrieve a non-existent price
    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);

    assertNull(retrievedEntry, "PriceEntry should be null for a non-existent symbol.");
  }

  /**
   * Test case for retrieving a price with a null symbol.
   * Ensures that the map handles null keys gracefully.
   */
  @Test
  void getPrice_nullSymbol_shouldNotReturnStoredEntryForNullKey() {
    String symbol = null;
    BigDecimal price = new BigDecimal("200.00");
    Instant timestamp = Instant.now();

    simplePriceStore.updatePrice(symbol, price, timestamp);

    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);
    assertNull(retrievedEntry, "PriceEntry should not be retrievable with a null key.");
  }

  /**
   * Test case for updating a price with an null symbol.
   */
  @Test
  void updatePrice_nullSymbol_shouldNotStorePriceEntry() {
    String symbol = "ETH-USD";
    BigDecimal price = new BigDecimal("1.25");
    Instant timestamp = Instant.now();

    simplePriceStore.updatePrice(symbol, price, timestamp);

    String newSymbol = null;

    simplePriceStore.updatePrice(newSymbol, price, timestamp);

    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);
    assertNotNull(retrievedEntry, "PriceEntry should not be null for an existing symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match.");
  }

  /**
   * Test case for updating a price with an empty symbol string.
   */
  @Test
  void updatePrice_emptySymbol_shouldNotStorePriceEntry() {
    String symbol = "ETH-USD";
    BigDecimal price = new BigDecimal("1.25");
    Instant timestamp = Instant.now();

    simplePriceStore.updatePrice(symbol, price, timestamp);

    String newSymbol = "";

    simplePriceStore.updatePrice(newSymbol, price, timestamp);

    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);
    assertNotNull(retrievedEntry, "PriceEntry should not be null for an existing symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match.");
  }

  /**
   * Test case for updating a price with a null price value.
   */
  @Test
  void updatePrice_nullPrice_shouldNotStoreNullPrice() {
    String symbol = "ETH-USD";
    BigDecimal price = new BigDecimal("1.25");
    Instant timestamp = Instant.now();

    simplePriceStore.updatePrice(symbol, price, timestamp);

    BigDecimal newPrice = null;

    simplePriceStore.updatePrice(symbol, newPrice, timestamp);

    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);
    assertNotNull(retrievedEntry, "PriceEntry should not be null for an existing symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match.");
  }

  /**
   * Test case for updating a price with a null timestamp.
   */
  @Test
  void updatePrice_nullTimestamp_shouldNotStoreNullTimestamp() {
    String symbol = "ETH-USD";
    BigDecimal price = new BigDecimal("1.25");
    Instant timestamp = Instant.now();

    simplePriceStore.updatePrice(symbol, price, timestamp);

    Instant newTimestamp = null;

    simplePriceStore.updatePrice(symbol, price, newTimestamp);

    PriceEntry retrievedEntry = simplePriceStore.getPrice(symbol);
    assertNotNull(retrievedEntry, "PriceEntry should not be null for an existing symbol.");
    assertEquals(symbol, retrievedEntry.getSymbol(), "Symbol should match.");
    assertEquals(price, retrievedEntry.getPrice(), "Price should match.");
    assertEquals(timestamp.toString(), retrievedEntry.getTimestamp(), "Timestamp should match.");
  }
}
