# StreamGuard

AI-Powered Real-Time Data Quality & Schema Evolution Platform.

StreamGuard monitors Kafka event streams, validates events against versioned data
contracts, detects schema drift and data-quality violations in real time, and uses
an LLM to recommend safe schema migrations and remediation actions.

## Problem

Modern event-driven systems break silently when producers change schemas without
coordinating with consumers. StreamGuard treats Kafka event schemas as versioned,
first-class contracts. Every event is validated against its contract before being
accepted, every contract change is classified for compatibility, and the LLM is
used *only* to recommend remediation on top of a deterministic analysis.

## Architecture

```
                         +-------------------------+
                         |      React Dashboard    |
                         +------------+------------+
                                      |
                                      v
                         +-------------------------+
                         |       API Gateway       |
                         +------------+------------+
                                      |
            +-------------------------+--------------------------+
            |                         |                          |
            v                         v                          v
   +----------------+     +------------------+       +---------------------+
   |  Contract      |     |  Validation      |       |  Recommendation     |
   |  Service       |     |  Service         |       |  Service            |
   +--------+-------+     +---------+--------+       +----------+----------+
            |                       |                           |
            v                       v                           v
      +-----------+           +---------+               +--------------+
      |  MongoDB  |           |  Kafka  |               |  LLM Gateway |
      +-----------+           +----+----+               +------+-------+
                                   |                           |
                        +----------+----------+                v
                        |                     |         Gemini / OpenAI
                        v                     v
                     Redis              violations
                                        (MongoDB)

                 +--------------------------------+
                 | Prometheus + Grafana           |
                 +--------------------------------+
```

## Technology Stack

| Layer            | Technology                          |
| ---------------- | ----------------------------------- |
| Language         | Java 21                             |
| Backend          | Spring Boot 3.5                     |
| Reactive         | Spring WebFlux                      |
| Messaging        | Apache Kafka 3.8 (KRaft mode)       |
| Database         | MongoDB 8                           |
| Cache            | Redis 7                             |
| AI               | Gemini / OpenAI (pluggable)         |
| Resilience       | Resilience4j (circuit breaker + retry) |
| Metrics          | Micrometer + Prometheus             |
| Dashboards       | Grafana                             |
| Containerization | Docker + Docker Compose             |
| Testing          | JUnit 5 + Mockito + Testcontainers  |
| Frontend         | React 18 + Vite + TypeScript        |
| Build            | Maven (multi-module)                |
| CI/CD            | GitHub Actions                      |
| Data lake        | AWS S3 (Parquet) + PySpark          |

## Event Flow

1. Producer publishes an `EventEnvelope` (`eventType`, `schemaVersion`, `payload`) to `order-events`.
2. `validation-service` consumes it, looks up the matching contract from Redis
   (falling back to MongoDB), and runs schema + quality-rule validation.
3. Validation outcome is published to `validation-results`. Failures also go to
   `order-events-dlq`.
4. When a new contract version is uploaded, `contract-service` computes a diff
   between versions and classifies compatibility deterministically.
5. For `BREAKING` or `POTENTIALLY_BREAKING` changes, the `recommendation-service`
   calls an LLM through a resilient WebFlux gateway and returns a *structured*
   JSON recommendation. A Java guardrail re-validates the LLM's output.

## Data Contract Model

```json
{
  "eventType": "ORDER_CREATED",
  "version": 1,
  "fields": {
    "eventId":    {"type": "STRING",  "required": true},
    "customerId": {"type": "STRING",  "required": true},
    "amount":     {"type": "DECIMAL", "required": true},
    "currency":   {"type": "STRING",  "required": true},
    "status":     {"type": "STRING",  "required": true}
  },
  "qualityRules": [
    {"field": "amount",   "operator": "GREATER_THAN", "expectedValue": "0"},
    {"field": "currency", "operator": "IN",           "expectedValue": "USD,CAD,EUR,GBP"}
  ]
}
```

## Schema Compatibility

Compatibility is **not** decided by the LLM.

| Change                      | Classification         |
| --------------------------- | ---------------------- |
| Add optional field          | COMPATIBLE             |
| Add required field          | BREAKING               |
| Remove optional field       | POTENTIALLY_BREAKING   |
| Remove required field       | BREAKING               |
| Change field type           | BREAKING               |
| Change required -> optional | COMPATIBLE             |
| Change optional -> required | BREAKING               |

## AI Recommendation Architecture

```
  Schema Diff  -->  Compatibility Engine  -->  Structured Analysis
                                                       |
                                                       v
                                                  LLM Gateway
                                                       |
                                                       v
                                               Structured JSON
                                                       |
                                                       v
                                                Guardrail Validator
```

The LLM returns a `MigrationRecommendation` JSON record. A Java guardrail
rejects/downgrades any recommendation that contradicts the deterministic
compatibility verdict (e.g. the LLM says `LOW` severity while Java says
`BREAKING`).

## Resilience Strategy

* All outbound LLM calls go through a Resilience4j circuit breaker + retry.
* On open circuit or exhausted retries, a fallback returns the deterministic
  compatibility verdict with a note that no LLM commentary is available.
* Kafka failures use a dead-letter topic and bounded retry topics.
* Idempotency keys in Redis (`processed:event:<eventId>`) protect against Kafka's
  at-least-once delivery.

## Kafka Design

| Topic                   | Purpose                             |
| ----------------------- | ----------------------------------- |
| `order-events`          | Primary event stream                |
| `validation-results`    | Validation outcomes                 |
| `order-events-dlq`      | Events that failed validation       |
| `order-events-retry-1`  | First retry                         |
| `order-events-retry-2`  | Second retry                        |
| `schema-drift-events`   | Detected schema drift notifications |

## Local Setup

```bash
docker compose up -d
mvn clean verify

# Start services in separate terminals
mvn -pl contract-service        spring-boot:run
mvn -pl validation-service      spring-boot:run
mvn -pl recommendation-service  spring-boot:run
mvn -pl api-gateway             spring-boot:run

# Dashboard
cd dashboard && npm install && npm run dev
```

Default ports:

| Service                | Port |
| ---------------------- | ---- |
| api-gateway            | 8080 |
| contract-service       | 8081 |
| validation-service     | 8082 |
| recommendation-service | 8083 |
| React dashboard        | 5173 |
| Prometheus             | 9090 |
| Grafana                | 3000 |

## API Highlights

```
POST   /api/contracts
GET    /api/contracts/{eventType}/{version}
GET    /api/contracts/{eventType}/compare?from=1&to=2
GET    /api/violations?eventType=ORDER_CREATED
POST   /api/recommendations
```

## Architecture Decisions

* **Why MongoDB?** Contracts are nested, versioned documents; a document store maps naturally.
* **Why Redis?** Contracts are read on every event; TTL caches avoid hammering MongoDB.
* **Why Kafka?** Decouples validation, enrichment, DLQ handling, and downstream analytics.
* **Why WebFlux in the recommendation path?** The LLM call is network-bound and benefits from non-blocking I/O.
* **Why keep compatibility deterministic?** AI should recommend, not decide.
  Deterministic rules keep the system auditable and reproducible.
* **Why DLQ + retry topics?** Separate concerns: retries vs. permanent failures.
* **Why structured JSON LLM output?** Makes guardrails and dashboards possible.

## Trade-offs

* The deterministic compatibility rules are intentionally conservative. They
  will sometimes classify compatible changes as `POTENTIALLY_BREAKING` when
  consumers tolerate the change — operators can override via the dashboard.
* The LLM is treated as *advisory*; its unavailability doesn't block the pipeline.
* All contracts are small enough to cache fully in Redis. Very large contracts
  would need a different strategy.

## Future Improvements

* OIDC/JWT authentication on the API gateway.
* Avro + Schema Registry adapter alongside the JSON contract model.
* Spark Structured Streaming job over the S3 lake for long-horizon trends.
* Protobuf support.
* Automatic contract inference from historical events.

## Project Layout

```
streamguard/
  common/                  -- shared domain models
  contract-service/        -- contracts, versions, diff, compatibility
  validation-service/      -- Kafka consumer, validation + quality rules, cache, DLQ, metrics, generator
  recommendation-service/  -- LLM gateway, prompt builder, guardrails
  api-gateway/             -- Spring Cloud Gateway + security
  data-lake-exporter/      -- S3 Parquet exporter
  sample-events/           -- contracts, events, PySpark job
  dashboard/               -- React + Vite + TypeScript
  infrastructure/          -- Prometheus + Grafana config
  .github/workflows/       -- CI
  docker-compose.yml
```