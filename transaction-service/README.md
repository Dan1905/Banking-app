# Transaction Service

Transaction orchestration service for deposits, withdrawals, and transfers.

## Responsibilities

- Validate and persist transactions
- Call account-service internal debit/credit endpoints
- Resolve customer email metadata through account-service and auth-service internal lookups
- Publish `TransactionEvent` to Kafka after successful DB commit

## Default Port

- `8083`

## Key Configuration

From `src/main/resources/application.properties`:

- `TRANSACTION_DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_API_TOKEN`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_TOPIC_TRANSACTION_EVENTS`
- `ACCOUNT_SERVICE_URL`
- `AUTH_SERVICE_URL`

## API Endpoints

Base path: `/api/transactions`

- `POST /api/transactions/transfer`
- `POST /api/transactions/deposit-withdrawal`
- `GET /api/transactions/{transactionId}`
- `GET /api/transactions/account/{accountNumber}`

## Internal Integrations

- Calls account-service internal APIs with `X-Internal-Token`
- Calls auth-service internal email lookup with `X-Internal-Token`
- Logs and falls back safely if email lookup fails (event still publishes with null email)

## Run Locally

```bash
mvn -pl transaction-service -am spring-boot:run
```

## Tests

```bash
mvn -B -pl transaction-service -am test -f pom.xml
```
