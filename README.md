# Weather Anomaly Auditor — Event-Driven Processing Engine

## Project focus

This is a single Java (Spring Boot) application that demonstrates building scalable, reliable, and auditable event processing pipelines. The project showcases patterns commonly required for high-throughput applications such as guaranteed delivery, polyglot persistence, and secured API access.

## Value proposition

- Guaranteed delivery of incoming events using RabbitMQ.
- Polyglot persistence (ScyllaDB / Cassandra for an immutable audit ledger; MySQL / MariaDB for structured results).
- Application-layer security using Spring Security for API endpoints.
- Event-driven architecture with clear separation of ingestion, processing, and storage.

## Technical stack

| Component | Technology | Purpose |
|---|---|---|
| Backend core | Java 21, Spring Boot | Main application and processing logic |
| Audit ledger | ScyllaDB / Cassandra | High-write throughput, immutable event ledger |
| Result store | MySQL / MariaDB | Structured, transactional storage for alerts and configuration |
| Messaging | RabbitMQ | Reliable queueing and decoupling between producers and consumers |
| Security | Spring Security | Secures API access |
| DevOps | Docker Compose / Linux | Reproducible local multi-service environment |

## Architectural flow and key features

I. Data ingestion and auditing

- Reliable consumption: a multi-threaded RabbitMQ consumer reads raw events from a queue.
- Immutable write: the raw event JSON is persisted to the audit ledger as a durable record.

II. Real-time anomaly processing

- Business logic detects anomalies by evaluating incoming event data against configured thresholds.
- Decoupled alerting: anomalies generate alert messages sent to a separate queue for review and further processing.

III. System security and data integrity

- Result processing: a consumer reads from the alert queue and stores structured alert records in MySQL.
- Secured API access: REST endpoints (for example, `/api/alerts`) are protected using Spring Security so only authenticated clients can query results.

## Getting started (development)

1. Start backend services required by the app (RabbitMQ, ScyllaDB, MySQL). Use Docker Compose or local services as appropriate.
2. Build and run the Spring Boot application with Gradle:

   ```bash
   # from repository root
   ./gradlew bootRun
   ```

## Frontend (optional)

This repository may contain a separate `frontend/` folder with a Vite + React app. During development, the Vite dev server can proxy API requests to the Spring Boot backend. To build the frontend for production, run the build script inside `frontend/` and copy the `dist` output into `src/main/resources/static` if you want Spring Boot to serve the static files.

## Next steps and suggestions

- Add a Gradle task that runs the frontend build and copies output into the backend resources during the Java build.
- Add CI workflows to build and test both frontend and backend artifacts.
- Document the required environment variables and Docker Compose configuration for local development and testing.