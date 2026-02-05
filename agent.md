# Agent.md - OpenTelemetry Spring Boot Demo

## Project Overview

**Name:** ot (OpenTelemetry Demo)  
**Type:** Spring Boot 4.0 Demo Application  
**Purpose:** Demonstrate OpenTelemetry integration with Spring Boot 4.0's new official starter  
**Author:** dev.danvega  
**Language:** Java 25  
**Framework:** Spring Boot 4.0.1  
**Build Tool:** Maven

## What This Project Does

This is a demonstration project showcasing the new `spring-boot-starter-opentelemetry` introduced in Spring Boot 4.0. It provides a complete example of:

1. **Distributed tracing** - Automatic trace collection for HTTP requests
2. **Metrics export** - Metrics sent to OTLP-compatible backends
3. **Log correlation** - Automatic trace/span ID injection into logs
4. **Database instrumentation** - Automatic JDBC query tracing
5. **Observability backend integration** - Complete setup with Grafana LGTM stack

The application demonstrates vendor-neutral observability using the OpenTelemetry Protocol (OTLP) to export telemetry data.

## Architecture

### Technology Stack

- **Java 25**
- **Spring Boot 4.0.1** with modular architecture
- **Spring Web MVC** - REST API endpoints
- **Spring Data JPA** - Database access with H2 in-memory database
- **OpenTelemetry** - Observability (traces, metrics, logs)
- **Logback** - Logging with OpenTelemetry appender
- **Docker Compose** - Local development with Grafana LGTM stack
- **Maven** - Build and dependency management

### Key Dependencies

```xml
spring-boot-starter-webmvc          # Web framework
spring-boot-starter-data-jpa        # Database access
spring-boot-starter-opentelemetry   # OpenTelemetry integration
opentelemetry-logback-appender-1.0  # Log instrumentation
opentelemetry-jdbc                  # Database instrumentation
spring-boot-docker-compose          # Auto-configure Docker services
```

### Observability Stack (LGTM)

- **Loki** - Log aggregation
- **Grafana** - Visualization and dashboards
- **Tempo** - Distributed tracing backend
- **Mimir** - Metrics storage (Prometheus)

Runs in Docker via `compose.yaml` on ports:
- 3000 - Grafana UI
- 4317 - OTLP gRPC endpoint
- 4318 - OTLP HTTP endpoint

## Project Structure

```
src/main/java/dev/danvega/ot/
├── Application.java                      # Main Spring Boot application
├── HomeController.java                   # REST endpoints with logging
├── InstallOpenTelemetryAppender.java     # Configures OTel log appender
├── DataLoader.java                       # Loads test data on startup
├── User.java                             # JPA entity
└── UserRepository.java                   # Spring Data repository

src/main/resources/
├── application.yaml                      # OTel configuration (disabled by org)
├── application.properties                # Alternative config
└── logback-spring.xml                    # Logging with OTel appender

compose.yaml                              # Grafana LGTM stack setup
pom.xml                                   # Maven dependencies
```

## Core Components

### 1. Application.java
Standard Spring Boot main class. No special OpenTelemetry configuration needed - it's all auto-configured!

### 2. HomeController.java
REST controller with three endpoints demonstrating tracing:
- `GET /` - Simple hello world
- `GET /greet/{name}` - Database lookup with simulated 50ms work
- `GET /slow` - 500ms delay to demonstrate slow operation tracing

All endpoints include structured logging with automatic trace context injection.

### 3. InstallOpenTelemetryAppender.java
**Important:** This component initializes the OpenTelemetry Logback appender at application startup.

```java
@Component
class InstallOpenTelemetryAppender implements InitializingBean {
    // Installs OTel appender and sets global OpenTelemetry instance
}
```

This ensures logs are correlated with traces by injecting trace/span IDs.

### 4. User Entity & Repository
Simple JPA setup with:
- `User` entity with id, name, and personalizedMessage fields
- `UserRepository` interface extending JpaRepository
- `DataLoader` to populate test data (Dan, Alice, Bob)

### 5. logback-spring.xml
Configures two appenders:
- `CONSOLE` - Standard console output
- `OTEL` - OpenTelemetry appender for log export

## Configuration

The application uses OpenTelemetry auto-configuration with these key settings:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% trace sampling (use 0.1 in production)
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
```

**Note:** Metrics and traces use different config paths due to how Spring Boot integrates Micrometer (metrics) vs OpenTelemetry (tracing), but both send to the same OTLP collector.

## How to Work With This Project

### Build and Run

```bash
# Build the project
./mvnw clean install

# Run the application (auto-starts Docker Compose)
./mvnw spring-boot:run
```

### Test Endpoints

```bash
# Simple endpoint
curl http://localhost:8080/

# Greeting with database lookup
curl http://localhost:8080/greet/Dan
curl http://localhost:8080/greet/Alice

# Slow operation
curl http://localhost:8080/slow
```

### View Observability Data

1. **Open Grafana:** http://localhost:3000
2. **View Traces:**
   - Go to Explore → Select Tempo
   - Search for service "ot"
   - Click traces to see span details
3. **View Metrics:**
   - Go to Explore → Select Prometheus
   - Query: `http_server_requests_seconds_count`
4. **View Logs:**
   - Go to Explore → Select Loki
   - Filter by job or trace ID

### Understanding Trace Context in Logs

Log format includes trace context:
```
2025-12-22T11:30:05.801-05:00  INFO [ot] [nio-8080-exec-2] [traceId-spanId] dev.danvega.ot.HomeController : Message
                                                              └─────────────┘
                                                              Trace correlation
```

- **Trace ID**: Identifies entire request flow across services
- **Span ID**: Identifies specific operation within the trace

## Key Concepts Demonstrated

### 1. Spring Boot 4.0 Modularization
The OpenTelemetry starter is possible due to Spring Boot 4's modular architecture, which allows optional, focused starters.

### 2. OTLP Protocol Over Library Lock-in
The demo emphasizes that **the protocol (OTLP) matters more than the library**. Spring Boot uses Micrometer internally but exports everything via OTLP, allowing you to use any OpenTelemetry-compatible backend.

### 3. Three Approaches to OpenTelemetry with Spring
1. **Java Agent** - Zero code changes, potential version issues
2. **Third-party OTel Starter** - From OTel project, alpha dependencies
3. **Spring Boot Starter** (this demo) - Official, stable, well-integrated

### 4. Actuator vs OpenTelemetry Starter
- **Actuator**: Health checks, production readiness, multiple protocols
- **OTel Starter**: Focused on telemetry, vendor-neutral, OTLP only
- **Not mutually exclusive**: Can use both together

## Common Development Tasks

### Adding New Endpoints
1. Add method to `HomeController.java` with `@GetMapping`
2. Include structured logging with `log.info()`
3. Traces and logs are automatically instrumented

### Modifying Trace Sampling
Edit `application.yaml`:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests
```

### Adding Custom Spans
Inject `Tracer` bean and create custom spans:
```java
@Autowired
private Tracer tracer;

public void myMethod() {
    Span span = tracer.spanBuilder("my-operation").startSpan();
    try (Scope scope = span.makeCurrent()) {
        // Your code here
    } finally {
        span.end();
    }
}
```

### Database Configuration
Currently uses H2 in-memory database. To switch to PostgreSQL or other databases:
1. Add dependency to `pom.xml`
2. Update `application.yaml` with connection details
3. JDBC instrumentation works automatically

### Adjusting Log Export
Modify `logback-spring.xml` to configure the OpenTelemetry appender behavior.

## Testing

The project includes:
- `ApplicationTests.java` - Basic context load test
- Test dependencies include `spring-boot-starter-opentelemetry-test`

Run tests:
```bash
./mvnw test
```

## Important Notes for AI Agents

1. **Don't modify `InstallOpenTelemetryAppender.java`** unless specifically asked - it's critical for log correlation
2. **Configuration file access restricted** - `application.yaml` is disabled by organization policy
3. **Auto-instrumentation is key** - Most observability features work without code changes
4. **Docker Compose integration** - The app automatically manages the Grafana LGTM container
5. **Java 25** - This project uses the latest Java version; ensure compatibility when suggesting changes

## Troubleshooting

### No traces appearing in Grafana
- Check Docker Compose is running: `docker ps`
- Verify OTLP endpoints are accessible: `curl http://localhost:4318/v1/traces`
- Check sampling probability is > 0

### Trace IDs not in logs
- Ensure `InstallOpenTelemetryAppender` component is loading
- Verify `logback-spring.xml` includes OTEL appender
- Check console for OpenTelemetry initialization messages

### Database not initialized
- `DataLoader` should run on startup
- Check logs for "Loading test data" message
- H2 console available at `/h2-console` if enabled

## Learning Resources

This demo is designed to teach:
- Spring Boot 4.0's modular architecture
- OpenTelemetry Protocol (OTLP) fundamentals
- Vendor-neutral observability patterns
- Distributed tracing in microservices
- Log correlation with traces
- Metrics export and visualization

## Future Enhancements

Potential improvements for this demo:
- Add custom metrics with Micrometer
- Demonstrate context propagation across services
- Add custom span attributes and events
- Include exemplars (linking metrics to traces)
- Add more complex distributed scenarios
- Demonstrate baggage propagation

---

**Last Updated:** February 5, 2026  
**Spring Boot Version:** 4.0.1  
**Java Version:** 25
