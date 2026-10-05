# Sample Events & Contracts

Hand-crafted samples for seeding the system and running end-to-end demos.

## Seed the contracts

```bash
curl -X POST http://localhost:8080/api/contracts \
     -H 'Content-Type: application/json' \
     -d @contracts/order-created-v1.json

curl -X POST http://localhost:8080/api/contracts \
     -H 'Content-Type: application/json' \
     -d @contracts/order-created-v2.json

curl -X POST http://localhost:8080/api/contracts \
     -H 'Content-Type: application/json' \
     -d @contracts/payment-completed-v1.json
```

## Compare versions

```bash
curl 'http://localhost:8080/api/contracts/ORDER_CREATED/compare?from=1&to=2'
```

## Publish events to Kafka

Using `kcat`:

```bash
jq -c '.[]' events/valid-orders.json | \
  kcat -b localhost:9092 -t order-events -P

jq -c '.[]' events/invalid-orders.json | \
  kcat -b localhost:9092 -t order-events -P
```

## Request an LLM recommendation

```bash
curl -X POST http://localhost:8080/api/recommendations \
     -H 'Content-Type: application/json' \
     -d "$(curl -s 'http://localhost:8080/api/contracts/ORDER_CREATED/compare?from=1&to=2')"
```

## Run the PySpark analytics job

```bash
spark-submit --packages org.apache.hadoop:hadoop-aws:3.4.1 \
  pyspark/quality_analytics.py s3a://streamguard-data/validation-results/
```