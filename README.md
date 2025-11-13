# Weather Anomaly Auditor — Event-Driven Processing Engine (Climate Watchtower)

## Project focus

This is a single Java (Spring Boot) application that demonstrates building scalable, reliable, and auditable event processing pipelines. The project showcases patterns commonly required for high-throughput applications such as guaranteed delivery, consolidated persistence on TimescaleDB/Postgres, and secured API access.

## Value proposition

- Guaranteed delivery of incoming events using RabbitMQ.
- Consolidated persistence using PostgreSQL/TimescaleDB for both the immutable audit ledger and structured results.
- Application-layer security using Spring Security for API endpoints.
- Event-driven architecture with clear separation of ingestion, processing, and storage.

## Technical stack

| Component | Technology | Purpose |
|---|---|---|
| Backend core | Java 21, Spring Boot | Main application and processing logic |
| Audit ledger & results | PostgreSQL / TimescaleDB | Time-series audit ledger and structured result storage |
| Messaging | RabbitMQ | Reliable queueing and decoupling between producers and consumers |
| Security | Spring Security | Secures API access |
| DevOps | Docker Compose / Linux | Reproducible local multi-service environment |

## Architectural flow and key features

I. Data ingestion and auditing

- Reliable consumption: a multi-threaded RabbitMQ consumer reads raw events from a queue.
- Immutable write: the raw event JSON is persisted to TimescaleDB as a durable record.

II. Real-time anomaly processing

- Business logic detects anomalies by evaluating incoming event data against configured thresholds.
- Decoupled alerting: anomalies generate alert messages sent to a separate queue for review and further processing.

III. System security and data integrity

- Result processing: a consumer reads from the alert queue and stores structured alert records in PostgreSQL/TimescaleDB.
- Secured API access: REST endpoints (for example, `/api/alerts`) are protected using Spring Security so only authenticated clients can query results.

## Getting started (development)

1. Start backend services required by the app (RabbitMQ, TimescaleDB). Use Docker Compose or local services as appropriate.
2. Build and run the Spring Boot application with Gradle:

   ```bash
   # from repository root
   ./gradlew bootRun
   ```