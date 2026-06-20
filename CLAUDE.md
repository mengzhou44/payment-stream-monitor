# payment-stream-monitor

A demo project that streams PostgreSQL payment changes into Elasticsearch via Debezium CDC and Kafka.

## Architecture

```
PaymentSimulator (every 2s)
        │
        ▼
   PostgreSQL  ←── Flyway manages schema
        │  WAL logical replication
        ▼
 Debezium Kafka Connect (PostgresConnector, pgoutput)
        │  topic: dbserver1.public.payments
        ▼
      Kafka (KRaft mode, CP 7.5.0)
        │                │
        │ (on failure)   │ (success)
        ▼                ▼
      DLT topic    PaymentCdcConsumer
  (after 3 retries)      │
        │                ▼
        │         PaymentServiceElasticsearch
        │                │
        └──── replay ────▼
                   Elasticsearch → Kibana (port 5601)
```

## Infrastructure (docker-compose.yml)

All services run on the `payment-network` bridge network.

| Service        | Image                              | Port  | Notes                          |
|----------------|------------------------------------|-------|--------------------------------|
| postgres       | postgres:15                        | 5432  | WAL level set to `logical`     |
| kafka          | confluentinc/cp-kafka:7.5.0        | 9092  | KRaft mode (no ZooKeeper)      |
| kafka-connect  | debezium/connect:2.4               | 8083  | Hosts the Postgres connector   |
| elasticsearch  | elasticsearch:8.11.0               | 9200  | Security disabled, single-node |
| kibana         | kibana:8.11.0                      | 5601  | Points to elasticsearch:9200   |

### Kafka KRaft configuration

ZooKeeper was removed in favour of KRaft. The single broker acts as both broker and controller:

- `KAFKA_PROCESS_ROLES: broker,controller`
- Controller quorum listener on internal port 9093
- `CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk` — required by Confluent image to format KRaft storage on first boot

## Spring Boot app

- **Port:** 4000
- **Java:** 17, Spring Boot 3.2.0
- **Build:** `mvn spring-boot:run`

### Package structure

All classes live flat under `com.demo.paymentstream.payment`:

| Class | Purpose |
|---|---|
| `Payment` | JPA entity — `id`, `paymentDate`, `BigDecimal amount` |
| `PaymentDocument` | Elasticsearch document — same fields |
| `PaymentRepository` | JPA repo (Postgres) |
| `PaymentRepositoryElasticsearch` | ES repo |
| `PaymentService` | Creates and retrieves payments in Postgres |
| `PaymentServiceElasticsearch` | Upserts and deletes documents in ES |
| `PaymentController` | REST API (see endpoints below) |
| `PaymentSimulator` | Inserts a random payment every 2 s |
| `PaymentCdcConsumer` | Kafka listener — routes CDC ops to `PaymentServiceElasticsearch` |
| `PaymentKafkaConfig` | Retry (3×, 1 s apart) + DLT error handler |
| `PaymentDlqService` | DLQ alert listener + count + replay |

### REST endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/payments` | Create a payment manually |
| `GET` | `/api/payments/{id}` | Fetch a payment by ID |
| `GET` | `/api/payments/dlq/count` | Number of messages currently in the DLQ |
| `POST` | `/api/payments/dlq/replay` | Replay all DLQ messages back to the original topic |

### CDC op handling

`PaymentCdcConsumer` maps Debezium ops:
- `c` (create), `u` (update), `r` (snapshot) → upsert into ES
- `d` (delete) → delete from ES
- `payment_date` arrives as microseconds since epoch and is converted to `Instant`
- `amount` is `BigDecimal` — parsed from the JSON text representation to avoid floating-point drift

### DLQ flow

1. `PaymentCdcConsumer` fails to write to ES (e.g. ES is down)
2. Spring Kafka retries 3 times with 1 s between attempts (`PaymentKafkaConfig`)
3. After all retries fail, message is sent to `dbserver1.public.payments.DLT`
4. `PaymentDlqService` logs a `WARN` alert with replay instructions
5. Once ES is back up: `POST /api/payments/dlq/replay` → messages flow back to original topic → consumer processes them successfully

## Schema management (Flyway)

Migrations in `src/main/resources/db/migration/`:

| File | Change |
|---|---|
| `V1__create_payments_table.sql` | Creates `payments` table |
| `V2__alter_payments_amount_to_numeric.sql` | Changes `amount` from `DOUBLE PRECISION` to `NUMERIC(19,4)` |

Flyway runs automatically on app startup before Hibernate initialises.

## Running locally

### 1. Start infrastructure

```bash
docker-compose up -d
```

### 2. Register the Debezium connector

Run immediately — the script waits until Kafka Connect is ready, then registers:

```bash
bash debezium/register-connector.sh
```

Only needs to be run once. Config is persisted in Kafka Connect internal topics.

### 3. Start the app

```bash
mvn spring-boot:run
```

Flyway applies any pending migrations, then the app starts. `PaymentSimulator` begins inserting payments every 2 seconds automatically.

### 4. Verify in Kibana

Open http://localhost:5601 → Stack Management → Data Views → create pattern `payments`, time field `paymentDate`.

## Configuration

`src/main/resources/application.yml`:

```yaml
server.port: 4000
spring.datasource.url: jdbc:postgresql://localhost:5432/payments_db
spring.kafka.bootstrap-servers: localhost:9092
spring.elasticsearch.uris: http://localhost:9200
debezium.topic: dbserver1.public.payments
debezium.dlq-topic: dbserver1.public.payments.DLT
```
