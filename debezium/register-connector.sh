#!/bin/bash
echo "Waiting for Kafka Connect to be ready..."
until curl -sf http://localhost:8083/connectors > /dev/null; do
  sleep 2
done

echo "Kafka Connect is ready. Registering payments connector..."
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payments-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "payments_db",
      "topic.prefix": "dbserver1",
      "table.include.list": "public.payments",
      "plugin.name": "pgoutput",
      "decimal.handling.mode": "double"
    }
  }'

echo ""
echo "Connector registered."
