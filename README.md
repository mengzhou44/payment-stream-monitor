# Payment Stream Monitor

A demo pipeline that streams PostgreSQL payment changes into Elasticsearch in real time using Debezium CDC and Kafka.

```
PostgreSQL → Debezium (Kafka Connect) → Kafka → Spring Boot → Elasticsearch → Kibana
```

## Prerequisites

- Docker + Docker Compose
- Java 17
- Maven

## Running the app

### Step 1 — Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL, Kafka (KRaft), Kafka Connect, Elasticsearch, and Kibana.

### Step 2 — Register the Debezium connector

```bash
bash debezium/register-connector.sh
```

Run this immediately after step 1 — no need to wait. The script polls Kafka Connect every 2 seconds until it is ready, then registers the connector automatically. Only needs to be run once — the config is persisted in Kafka Connect.

### Step 4 — Start the Spring Boot app

```bash
mvn spring-boot:run
```

The app starts on port **4000** and immediately begins inserting a random payment into Postgres every 2 seconds. Changes flow through to Elasticsearch within a few seconds.

## Verify it's working

**Check the connector is running:**
```bash
curl http://localhost:8083/connectors/payments-connector/status
```

**Create a payment manually:**
```bash
curl -X POST http://localhost:4000/api/payments \
  -H "Content-Type: application/json" \
  -d '{"amount": 99.99}'
```

**Browse payments in Kibana:**

Open http://localhost:5601, go to *Stack Management → Index Patterns*, and create a pattern for `payments`.

## DLQ demo

This demo shows what happens when Elasticsearch goes down — messages are retried then parked in the Dead Letter Topic (DLT), and can be replayed once ES is back.

### Step 1 — Stop Elasticsearch

```bash
docker stop elasticsearch
```

### Step 2 — Watch messages accumulate in the DLQ

The app retries each failed message 3 times (1 second apart), then sends it to the DLT. Poll the count:

```bash
curl http://localhost:4000/api/payments/dlq/count
```

You should see the number climbing. The app also logs a `WARN` alert for each message that lands in the DLQ.

### Step 3 — Bring Elasticsearch back up

```bash
docker start elasticsearch
```

Wait ~10 seconds for ES to be ready.

### Step 4 — Replay the DLQ

```bash
curl -X POST http://localhost:4000/api/payments/dlq/replay
```

This reads all pending messages from the DLT and republishes them to the original Kafka topic. The CDC consumer picks them up and writes them to ES successfully.

### Step 5 — Verify the DLQ is empty

```bash
curl http://localhost:4000/api/payments/dlq/count
```

Should return `0 message(s) in DLQ`.

---

## Service ports

| Service       | URL                        |
|---------------|----------------------------|
| Spring Boot   | http://localhost:4000      |
| Kafka         | localhost:9092             |
| Kafka Connect | http://localhost:8083      |
| Elasticsearch | http://localhost:9200      |
| Kibana        | http://localhost:5601      |
| PostgreSQL    | localhost:5432             |

## Stopping

```bash
docker-compose down
```
