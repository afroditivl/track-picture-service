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

**9.** In Terminal 1, press **`Ctrl+C`** to stop the app.

**10.** Navigate to the project root and shut down containers:

```
cd /path/to/track-picture-service
docker compose down
```

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

## API

| Method | Path                                  | Description                                                        |
|--------|---------------------------------------|--------------------------------------------------------------------|
| GET    | `/picture`                            | All tracks in the current picture                                  |
| GET    | `/tracks/{trackId}`                   | Latest state; 404 if unknown                                       |
| GET    | `/tracks/{trackId}/history?from=&to=` | History rows; optional ISO-8601 UTC filters; 400 on bad timestamps |
