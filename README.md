# OpenTelemetry with Spring Boot 4.0 Demo

This demo showcases the new `spring-boot-starter-opentelemetry` introduced in Spring Boot 4.0, providing a simplified 
way to integrate OpenTelemetry observability into your Spring Boot applications.

## What's New in Spring Boot 4.0

Spring Boot 4.0 introduces an official OpenTelemetry starter from the Spring team. Unlike previous approaches that
required multiple dependencies and complex configuration, this starter provides:

> **How was this possible?** The [modularization of Spring Boot](https://spring.io/blog/2025/10/28/modularizing-spring-boot)
> in version 4.0 enabled the team to create focused, optional starters like this one. To learn more about Spring Boot 4's
> modular architecture, check out the [modularization examples](https://github.com/danvega/sb4/tree/master/features/modularization).

- **Single dependency**: Just add `spring-boot-starter-opentelemetry`
- **Automatic OTLP export**: Metrics and traces are exported via the OTLP protocol
- **Micrometer integration**: Uses Micrometer's tracing bridge to export traces in OTLP format
- **Vendor-neutral**: Works with any OpenTelemetry-capable backend (Grafana, Jaeger, etc.)

### Why This Approach?

There are three ways to use OpenTelemetry with Spring Boot:

1. **OpenTelemetry Java Agent** - Zero code changes but can have version compatibility issues
2. **Third-party OpenTelemetry Starter** - From the OTel project, but pulls in alpha dependencies
3. **Spring Boot Starter (this demo)—** - Official Spring support, stable, well-integrated

The key insight is that **it's the protocol (OTLP) that matters**, not the library.
Spring Boot uses Micrometer internally but exports everything via OTLP to any compatible backend.

### What About Spring Boot Actuator?

Spring Boot Actuator is Spring's traditional approach to observability and production readiness. Here's how it compares:

| Aspect | Spring Boot Actuator | OpenTelemetry Starter |
|--------|---------------------|----------------------|
| **Protocol** | Prometheus, OTLP, JMX, + many others | OTLP (vendor-neutral) |
| **Distributed Tracing** | Built-in via Micrometer Tracing (add bridge dependency) | Built-in, automatic |
| **Backend Lock-in** | Vendor-neutral via Micrometer (supports 15+ backends including OTLP) | Works with any OTLP backend |
| **Health Checks** | Built-in `/actuator/health` | Not included (requires Actuator) |
| **Production Readiness** | Full suite (info, env, beans, metrics, etc.) | Focused on telemetry only |
| **Setup Complexity** | More endpoints to configure/secure | Single OTLP endpoint |
| **Dependencies (Spring Boot 4)** | `spring-boot-starter-actuator` + bridge deps | `spring-boot-starter-opentelemetry` |

**Choose Actuator when:**
- You need health checks, readiness/liveness probes for Kubernetes
- You want to expose application info, environment, or bean details
- Your monitoring stack is already Prometheus-based with scraping

**Choose OpenTelemetry Starter when:**
- You want vendor-neutral observability (easily switch backends)
- Distributed tracing across services is a priority
- You prefer push-based telemetry to pull-based scraping

**Note:** They're not mutually exclusive—many production apps use both (Actuator for health/readiness, OTel for telemetry).

## Prerequisites

- Java 17+
- Maven
- Docker (for Grafana LGTM stack)

The LGTM stack is Grafana Labs' open-source observability stack. The acronym stands for:

- Loki — for logs (log aggregation system)
- Grafana — for visualization and dashboards
- Tempo — for traces (distributed tracing backend)
- Mimir — for metrics (long-term storage for Prometheus metrics)

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
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
```

### Configuration Notes

- **sampling.probability**: Set to `1.0` for development (all traces). Use lower values in production (default is `0.1`)
- **Port 4318**: HTTP OTLP endpoint (use 4317 for gRPC)
- The `spring-boot-docker-compose` module auto-configures these endpoints when using Docker Compose

### Understanding the OTLP Export Configuration

**`management.otlp.metrics.export.url`** — Tells Spring Boot where to send **metrics** (counts, gauges, histograms like request counts, response times, memory usage). The data goes to an OTLP-compatible collector.

**`management.opentelemetry.tracing.export.otlp.endpoint`** — Tells Spring Boot where to send **traces** (timing/flow data showing how requests move through your app, spans showing each operation and duration).

**Why two separate configs?** Spring Boot's observability evolved over time:
- Metrics use Micrometer's OTLP exporter (hence `management.otlp.metrics`)
- Traces use the OpenTelemetry tracing bridge (hence `management.opentelemetry.tracing`)

Both send data to the same collector (port 4318), but the configuration paths differ due to how the libraries are integrated.

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
2025-12-22T11:30:05.801-05:00  INFO 13165 --- [ot] [nio-8080-exec-2] [f64d5e13e35ac0429ad2974512a3c4a7-2d46fc21f9ddfb2d] dev.danvega.ot.HomeController            : Greeting user: World
```

The trace context appears as `[traceId-spanId]` (e.g., `f64d5e13e35ac0429ad2974512a3c4a7-2d46fc21f9ddfb2d`).

### Understanding Trace and Span IDs

- **Trace ID** (`f64d5e13e35ac0429ad2974512a3c4a7`): Identifies the entire request flow across all services. Every log and span from a single request shares this ID.
- **Span ID** (`2d46fc21f9ddfb2d`): Identifies a specific operation within the trace. A single request may have multiple spans (controller → database → external API).

### Why This Matters

1. **Jump from trace to logs**: In Grafana, when viewing a trace in Tempo, you can click on a span to see only the logs emitted during that exact operation—no more searching through thousands of log lines.

2. **Jump from logs to trace**: If you spot an error in your logs, copy the trace ID and search for it in Tempo to see the full request flow and pinpoint where it failed.

3. **Debug specific operations**: If a request calls multiple services or performs several operations, the span ID tells you exactly which part of the request generated a particular log entry.

### Example: Multi-Span Request

```
Request: GET /greet/World
├── Span A (controller) ─── logs show span A's ID
│   └── Span B (simulateWork) ─── logs show span B's ID
```

When you see a log with span B's ID, you know it happened during `simulateWork()`, not the controller method.

## Exporting Logs via OTLP

This demo is configured to export logs to Grafana/Loki via OTLP. This allows you to view, search, and correlate logs with traces directly in Grafana.

### Important: Spring Boot Version Requirement

**Spring Boot 4.0.1 or later is required** for OTLP logging export without the actuator module.

In Spring Boot 4.0.0, the `OtlpLoggingAutoConfiguration` was part of the actuator module, requiring you to add `spring-boot-starter-actuator` just to export logs. This was fixed in [issue #48488](https://github.com/spring-projects/spring-boot/issues/48488) and released in Spring Boot 4.0.1, which moved the logging export auto-configuration to the core OpenTelemetry module.

### How It Works

Spring Boot provides auto-configuration for exporting logs in OTLP format, but does not install an appender into Logback by default. To enable log export, you need:

#### 1. Add the Logback Appender Dependency

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>2.21.0-alpha</version>
</dependency>
```

> **Note:** The `-alpha` suffix indicates this is still marked as unstable by the OpenTelemetry project. There are currently no stable (non-alpha) versions of this appender.

#### 2. Create `logback-spring.xml`

Create `src/main/resources/logback-spring.xml` to add the OpenTelemetry appender:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>

    <appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="OTEL"/>
    </root>
</configuration>
```

This configuration:
- Imports the default Spring Boot logging configuration
- Adds an OpenTelemetry appender that sends logs to the OpenTelemetry SDK
- Attaches both the console and OTEL appenders to the root logger

#### 3. Create the Appender Installation Bean

The OpenTelemetry appender needs to know which `OpenTelemetry` instance to use. Create this component:

```java
@Component
class InstallOpenTelemetryAppender implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }
}
```

This bean:
- Gets the auto-configured `OpenTelemetry` instance injected by Spring Boot
- Installs it into the Logback appender after the Spring context is ready
- Enables the appender to send logs through the OpenTelemetry SDK to the OTLP endpoint

#### 4. Docker Compose Auto-Configuration

When using `spring-boot-docker-compose` with the `grafana/otel-lgtm` image, Spring Boot **automatically configures** the OTLP endpoints for metrics, traces, and logs. You don't need to specify explicit endpoint URLs in `application.yaml`.

The auto-configuration detects the running container and configures:
- Metrics endpoint: `http://localhost:4318/v1/metrics`
- Traces endpoint: `http://localhost:4318/v1/traces`
- Logs endpoint: `http://localhost:4318/v1/logs`

### Viewing Logs in Grafana

1. Open http://localhost:3000
2. Go to **Explore** (compass icon)
3. Select **Loki** as the data source
4. Query for logs: `{service_name="ot"}`
5. Click on a log entry to see its trace context, then click the trace ID to jump directly to the trace in Tempo

### Manual Configuration (Optional)

If you're not using Docker Compose auto-configuration, you can explicitly set the endpoint:

```yaml
management:
  opentelemetry:
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
```

For more details, see the [OpenTelemetry with Spring Boot blog post](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot#exporting-logs).

## Next Steps

To extend this demo, you could:

1. **Add custom spans** using `@Observed` annotation:

```java
@Observed(name = "my-operation")
public void myMethod() {
    // ...
}
```

2. **Add trace ID to responses** using a servlet filter:

```java
@Component
class TraceIdFilter extends OncePerRequestFilter {
    private final Tracer tracer;

    // ... inject tracer and add X-Trace-Id header
}
```

3. **Call other services** to see distributed tracing across multiple applications

## Resources

- [OpenTelemetry with Spring Boot - Spring Blog](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot)
- [Spring Boot Tracing Documentation](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
