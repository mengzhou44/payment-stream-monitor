# Kibana Payments Dashboard Guide

## Prerequisites

- Infrastructure running (`docker-compose up -d`)
- App running (`mvn spring-boot:run`)
- Debezium connector registered (`bash debezium/register-connector.sh`)
- Kibana available at http://localhost:5601

---

## 1. Create a Data View

A data view tells Kibana which Elasticsearch index to read from.

1. Go to **Stack Management** (bottom of left sidebar)
2. Click **Data Views** → **Create data view**
3. Fill in:
   - **Name:** `payments`
   - **Index pattern:** `payments`
   - **Timestamp field:** `paymentDate`
4. Click **Save data view to Kibana**

---

## 2. Create the Dashboard

1. Go to **Analytics** → **Dashboard**
2. Click **Create dashboard**

---

## 3. Add a Payments Table

Shows one payment per row with all fields.

1. Click **Add panel** (top right) → **Create visualization**
2. In Lens, click the chart type dropdown (top left) → select **Table**
3. Make sure the data view (top left) is set to `payments`
4. Drag these fields from the left panel into the **Columns** area:
   - `id`
   - `paymentDate`
   - `customerId`
   - `countryCode`
   - `amount`
5. For each column, set the function to **Top values**
6. In the right panel, set **Number of values** to `100`
7. Click **Save and return**

---

## 4. Add a Bar Chart — Total Amount per Customer

1. Click **Add panel** → **Create visualization**
2. Chart type → **Bar vertical**
3. Data view → `payments`
4. **Horizontal axis** → drag `customerId`, function = **Top values**
5. **Vertical axis** → drag `amount`, function = **Sum**
6. Click **Save and return**

---

## 5. Add a Pie Chart — Payment Volume by Country

1. Click **Add panel** → **Create visualization**
2. Chart type → **Pie**
3. Data view → `payments`
4. **Slice by** → drag `countryCode`, function = **Top values**
5. **Size by** → drag `amount`, function = **Count**
6. Click **Save and return**

---

## 6. Add a Line Chart — Payment Trend Over Time

1. Click **Add panel** → **Create visualization**
2. Chart type → **Line**
3. Data view → `payments`
4. **Horizontal axis** → drag `paymentDate` (Lens auto-buckets by time interval)
5. **Vertical axis** → drag `amount`, function = **Sum**
6. Click **Save and return**

---

## 7. Add Metric KPIs

1. Click **Add panel** → **Create visualization**
2. Chart type → **Metric**
3. Data view → `payments`
4. Drag `amount` → function = **Sum** (total volume)
5. Save and return, then repeat for **Count** (total number of payments)

---

## 8. Save the Dashboard

1. Click **Save** (top right)
2. Name it `Payments Dashboard`
3. Click **Save**

---

## Searching Payments via REST

You can also query payments directly from the API:

```bash
# All payments
curl http://localhost:4000/api/payments/search

# By customer
curl http://localhost:4000/api/payments/search?customerId=CUST-001

# By country
curl http://localhost:4000/api/payments/search?countryCode=US

# By customer and country
curl http://localhost:4000/api/payments/search?customerId=CUST-001&countryCode=US
```

---

## Why customerId and countryCode Work in Aggregations

Both fields are mapped as `Keyword` type in Elasticsearch (`PaymentDocument.java`). `Keyword` fields are stored as-is (no tokenization), which allows Kibana to group and aggregate on them. If they were `Text` type, Kibana could not build bar charts or pie charts on them.
