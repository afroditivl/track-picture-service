# track-picture-service

Consume track updates from Kafka, keep an in-memory picture (latest state per track) and PostgreSQL
history, expose read APIs.

## Steps to run locally

You need Java 17, Maven, and Docker. Use two terminals, terminal 1 runs the app and must stay open.

Replace `/path/to/track-picture-service` with the folder where you cloned this repo.

### Terminal 1 — Docker + application

**1.** Navigate to the project root:

```
cd /path/to/track-picture-service
```

**2.** Start Kafka and PostgreSQL:

```
docker compose up -d
```

Wait some seconds until they are Started.

**3.** Start the Spring Boot app:

```
mvn spring-boot:run
```

Leave Terminal 1 open. When you see **`Started TrackPictureApplication`**, the API is on
**http://localhost:8080**.

### Terminal 2 — Demo data + checks

**4.** Open a new terminal. Navigate to the demo producer:

```
cd /path/to/track-picture-service/tools/demo-producer
```

**5.** Publish sample track updates to Kafka:

```
mvn exec:java
```

You should see lines ending with `Publishing done`.

**6.** Verify the API by running the following command:

```
curl -s http://localhost:8080/picture
```

Expect **3** tracks: `T-001`, `T-002`, `T-003` (no `T-004`).

```
curl -s http://localhost:8080/tracks/T-002
```

Expect `"timestamp":"2026-10-01T12:10:00Z"` and `"messageId":"msg-t002-newer"`.

```
curl -s http://localhost:8080/tracks/T-002/history
```

Expect **2** history rows (`msg-t002-older`, then `msg-t002-newer`).

```
curl -s http://localhost:8080/tracks/T-001/history
```

Expect **2** rows (`msg-t001-a`, `msg-t001-b` only — duplicate `messageId` is ignored).

```
curl -i http://localhost:8080/tracks/T-404
```

Expect **`HTTP/1.1 404`**.

### Stop

**7.** In Terminal 1, press **`Ctrl+C`** to stop the app.

**8.** Navigate to the project root and shut down containers:

```
cd /path/to/track-picture-service
docker compose down
```

---

## Architecture

```
  demo-producer ──► Kafka (track-updates) ──► TrackUpdateListener
                                                    │
                                                    ▼
                                           TrackUpdateProcessor
                                              /          \
                                             ▼            ▼
                                      PictureStore   HistoryRepository
                                      (in-memory)    (PostgreSQL)
                                             \            /
                                              ▼          ▼
                                         TrackController (REST)
                                              +
                                    Actuator (health, prometheus)
```

One Spring Boot process: Kafka consumer and HTTP API in the same JVM.

## Design decisions

| Area             | Choice                                                                  | Why                                                                           |
|------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Picture          | In-memory map (`PictureStore`)                                          | Fast reads; assignment allows holding latest state in memory                  |
| History          | PostgreSQL + JDBC                                                       | Durable audit trail; query by track and optional time range                   |
| Idempotency      | `message_id` primary key, `ON CONFLICT DO NOTHING`                      | Same `messageId` twice -> one history row; safe redelivery                    |
| Ordering         | Per-`trackId` lock; picture updated only if timestamp is strictly newer | Out-of-order updates still stored in history but do not roll back the picture |
| Invalid messages | Log and skip (no dead-letter topic)                                     | Keeps scope small; see out of scope below                                     |
| Schema           | `CREATE TABLE IF NOT EXISTS` on startup                                 | No migration tool for this exercise                                           |
| Tests            | Unit tests on validator and processor                                   | Covers validation rules and ordering/idempotency without Docker               |

## Trade-offs

- **Picture is empty after restart** — not rebuilt from Kafka (stretch goal not implemented).
- **Invalid updates are only logged** — not routed to a dead-letter topic; harder to inspect
  failures in ops tooling.
- **No consumer lag metric** — Prometheus exposes standard JVM/HTTP/Kafka client metrics; lag is not
  computed as a dedicated gauge.
- **Local credentials in `application.yml`** — acceptable for docker-compose demo only, not for
  production.
- **No integration tests** — manual runbook + unit tests on core logic; no Testcontainers end-to-end
  suite.

## Out of scope (with more time)

- Dead-letter Kafka topic for invalid messages (instead of log-only).
- Rebuild in-memory picture from `track-updates` on startup.
- More metrics or dashboard on top of Prometheus. eg track_updates_processed_total,
  track_updates_rejected_total, track_update_processing_seconds (for latency)
- Increate the unit tests coverage (and a JdbcTest for the queries).
- Add javadocs to public classes/methods.

---

## Kafka message format

Topic: **`track-updates`**. Key: **`trackId`** (recommended). Value: JSON:

```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "trackId": "T-001",
  "timestamp": "2026-10-01T12:00:00Z",
  "latitude": 51.5,
  "longitude": -0.1,
  "altitude": 1000,
  "heading": 270,
  "identity": "UNKNOWN"
}
```

`messageId` is an application idempotency key (not in the original sensor payload; added so
duplicates/redelivery are safe). `identity`: `UNKNOWN`, `FRIEND`, `NEUTRAL`, `HOSTILE`.

## API (http://localhost:8080)

| Method | Path                                  | Description                                                        |
|--------|---------------------------------------|--------------------------------------------------------------------|
| GET    | `/actuator/health`                    | Application health (`status`: UP or DOWN)                          |
| GET    | `/actuator/prometheus`                | Prometheus scrape format (Micrometer metrics)                      |
| GET    | `/picture`                            | All tracks in the current picture                                  |
| GET    | `/tracks/{trackId}`                   | Latest state; 404 if unknown                                       |
| GET    | `/tracks/{trackId}/history?from=&to=` | History rows; optional ISO-8601 UTC filters; 400 on bad timestamps |
