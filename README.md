# Price Aggregator

The Price Aggregator is a lightweight cryptocurrency price server designed to consume real-time ticker data from different exchanges and expose the latest prices through a REST API endpoint.

## Features:

- Real-time Data Consumption: Connects to WebSocket APIs to subscribe to and consume ticker details for specified cryptocurrency pairs.
- Supported Symbols: Tracks real-time prices for BTC-USD, ETH-USD, ETH-BTC etc (configurable)
- REST API Endpoint: Provides a /prices/{symbol} endpoint to retrieve the most recent price.
- Returns the latest price for the requested symbol.

## Building
To run tests:
```
./gradlew clean test
```

To build and package the application, use the Gradle wrapper:
```
./gradlew clean assemble
```

## Running the Application
### Locally (using Gradle)
Run the application directly using Gradle:
```
./gradlew clean run
```

The application will start and listen on port 8888 by default.

### With Docker
The application can be easily containerized using Docker, providing a consistent and isolated runtime environment.

#### Build the Docker Image:
Navigate to the root directory of your project where the Dockerfile is located.
```
docker build -t price-aggregator .
```

This command builds a Docker image named price-aggregator.

#### Run the Docker Container:
Once the image is built, you can run the container, mapping the application's port (8888) from the container to a port on your host machine (e.g., 8888):
```
docker run -p 8888:8888 --name price-aggregator price-aggregator
```
The application will now be accessible on http://localhost:8888.

## API Endpoint Usage

Once the server is running, you can access the price data via the REST API:
```
Endpoint: GET /prices/{symbol}
```
Symbol Convention: BASE-QUOTE (e.g., BTC-USD, ETH-USD, ETH-BTC)

Example Request:
```
GET http://localhost:8888/prices/BTC-USD
```
Example Success Response (HTTP 200 OK):
```
{
  "symbol": "BTC-USD",
  "price": "65000.50",
  "timestamp": "2025-07-14T17:30:00Z"
}
```
Example Error Response (HTTP 404 Not Found):
```
{
  "error": "Symbol not available"
}
```
## Debugging
The MainVerticle includes a main method for convenient debugging from an IDE. You can set breakpoints and run the application directly from your IDE to step through the code.

## Future improvements
- Use a smaller Docker base image for runtime such as `eclipse-temurin:17-jdk-alpine` to reduce image size and improve startup times.
- Extend configuration options to include server port, number of verticle deployment instances, logging levels etc.

## Design decisions
- Vert.x was chosen for its lightweight, event-driven, and non-blocking architecture, which enables high concurrency and low resource usage — useful for handling multiple WebSocket streams.
- The Price Store uses an in-memory ConcurrentHashMap for thread-safety and fast read/write access which fits well for a single-node. To scale, this could be replaced by a Redis cache.
- Prices are returned as strings instead of numeric types to avoid precision loss and rounding issues. Sending as a string removes those risks.

## Scaling
- Deploy on a Kubernetes cluster for horizontal scaling.
- Using a shared Redis cache allows multiple instances to share price data.
