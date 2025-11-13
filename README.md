# Climate Watchtower / Weather Anomaly Auditor

I’m building a distributed network of sensor modules (starting with weather). I wanted a system that can do two things for me:

1. Alert me when climate conditions shift suddenly.
2. Give me a trace of where a problem originates if something looks off.

This project is the processing and auditing engine for that. It watches real-time events coming in through RabbitMQ and keeps an immutable record so I can go back and see what happened and when.

## What it does right now

- Listens to weather events from my sensor network (more sources coming later).
- Stores raw JSON events in a time-series database (TimescaleDB/Postgres) like an audit log I don’t mutate.
- Evaluates incoming data against thresholds to decide if something is an anomaly.
- Publishes alert messages when it detects those anomalies.
- Exposes secured API endpoints so I can query alerts.

Example: it can tell me something like “this week’s average temperature is down ~3°C compared to last week.” That kind of simple comparative insight will expand as I feed it more data types.

## Where it’s going next

- I’ll gradually plug in new sensor types as I expand the network (humidity, pressure, wind, air quality, etc.).
- I’ll enrich the local sensor data with external climate APIs to tighten the quality of warnings.
- I’ll add health and heartbeat events from the remote nodes. Those will increase event volume.
- I plan to use a Phi accrual failure detection style approach to tell me if a node is failing, delaying, or dead.
- More derived metrics: week-over-week deltas, rolling volatility, gap detection, maybe seasonal baselines.

## Tech

- Language: Java 21 + Spring Boot
- Messaging: RabbitMQ
- Storage: PostgreSQL / TimescaleDB (raw ledger + structured results)
- Security: Spring Security (lock down endpoints like `/api/alerts`)
- Local dev: Docker Compose (RabbitMQ + DB)

## Running it (dev)

Make sure RabbitMQ and Postgres/TimescaleDB are running (use Docker Compose or your own instances).

```bash
# from repository root
./gradlew bootRun
```

Then hit the API (secured) once you have credentials set up.

## Why an audit ledger?

I don’t want to lose raw sensor input or silently patch history. If something goes wrong I need to trace back through exactly what the system saw. That’s why raw events are written immutably and alerts are a separate structured view.

## Future notes

As heartbeat data flows in, load will increase. I’ll tune consumers and queue settings, maybe add backpressure or dead-letter flows if needed. Scaling will be iterative based on what the real sensor traffic looks like, not theoretical charts.

---
This is a living project. I’ll keep shaping it as the sensor network grows.
