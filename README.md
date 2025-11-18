# OpenTelemetry with Spring Boot 4.0 Demo

This demo showcases the new `spring-boot-starter-opentelemetry` introduced in Spring Boot 4.0, providing a simplified 
way to integrate OpenTelemetry observability into your Spring Boot applications.

## What's New in Spring Boot 4.0

Spring Boot 4.0 introduces an official OpenTelemetry starter from the Spring team. Unlike previous approaches that 
required multiple dependencies and complex configuration, this starter provides:

- **Single dependency**: Just add `spring-boot-starter-opentelemetry`
- **Automatic OTLP export**: Metrics and traces are exported via the OTLP protocol
- **Micrometer integration**: Uses Micrometer's tracing bridge to export traces in OTLP format
- **Vendor-neutral**: Works with any OpenTelemetry-capable backend (Grafana, Jaeger, etc.)

### Why This Approach?

There are three ways to use OpenTelemetry with Spring Boot:

1. **OpenTelemetry Java Agent** - Zero code changes but can have version compatibility issues
2. **3rd-party OpenTelemetry Starter** - From the OTel project, but pulls in alpha dependencies
3. **Spring Boot Starter (this demo)** - Official Spring support, stable, well-integrated

The key insight is that **it's the protocol (OTLP) that matters**, not the library. Spring Boot uses Micrometer internally but exports everything via OTLP to any compatible backend.

## Prerequisites

- Java 17+
- Maven
- Docker (for Grafana LGTM stack)

## Project Structure

```
ot/
├── pom.xml                 # Spring Boot 4.0-RC2 with opentelemetry starter
├── compose.yaml            # Grafana LGTM stack (logs, metrics, traces)
└── src/
    └── main/
        ├── java/dev/danvega/ot/
        │   ├── OtApplication.java
        │   └── HomeController.java
        └── resources/
            └── application.yaml
```

## Dependencies

The key dependency is the new OpenTelemetry starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

This starter includes:
- OpenTelemetry API
- Micrometer tracing bridge to OpenTelemetry
- OTLP exporters for metrics and traces

## Configuration

```yaml
spring:
  application:
    name: ot

management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for development
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
```

### Configuration Notes

- **sampling.probability**: Set to `1.0` for development (all traces). Use lower values in production (default is `0.1`)
- **Port 4318**: HTTP OTLP endpoint (use 4317 for gRPC)
- The `spring-boot-docker-compose` module auto-configures these endpoints when using Docker Compose

## Running the Demo

1. **Start the application:**

```bash
./mvnw spring-boot:run
```

This automatically starts the Grafana LGTM container via Docker Compose.

2. **Generate some traces:**

```bash
# Simple endpoint
curl http://localhost:8080/

# Greeting with path variable
curl http://localhost:8080/greet/World

# Slow operation (500ms)
curl http://localhost:8080/slow
```

3. **View traces in Grafana:**

- Open http://localhost:3000
- Go to **Explore** (compass icon)
- Select **Tempo** as the data source
- Click **Search** and select service "ot"
- Click on a trace to see the span details

4. **View metrics:**

- In Grafana, go to **Explore**
- Select **Prometheus** as the data source
- Query for metrics like `http_server_requests_seconds_count`

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /` | Simple hello world response |
| `GET /greet/{name}` | Returns greeting with simulated 50ms work |
| `GET /slow` | Simulates a 500ms slow operation |

## What You Get Automatically

With `spring-boot-starter-opentelemetry`, you get automatic instrumentation for:

- HTTP server requests (all controller endpoints)
- HTTP client requests (RestTemplate, RestClient, WebClient)
- JDBC database calls
- Trace/span IDs in logs

## Viewing Logs with Trace Context

Your application logs automatically include trace and span IDs. Look for log entries like:

```
2025-11-18 10:30:45 [traceId=abc123, spanId=def456] INFO  d.d.o.HomeController - Greeting user: World
```

This allows you to correlate logs with traces in Grafana.

## Next Steps

To extend this demo, you could:

1. **Add custom spans** using `@Observed` annotation:

```java
@Observed(name = "my-operation")
public void myMethod() {
    // ...
}
```

2. **Export logs via OTLP** by adding a Logback appender (see the Spring blog post)

3. **Add trace ID to responses** using a servlet filter:

```java
@Component
class TraceIdFilter extends OncePerRequestFilter {
    private final Tracer tracer;

    // ... inject tracer and add X-Trace-Id header
}
```

4. **Call other services** to see distributed tracing across multiple applications

## Resources

- [OpenTelemetry with Spring Boot - Spring Blog](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot)
- [Spring Boot Tracing Documentation](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
