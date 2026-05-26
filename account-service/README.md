# Account Service

Account domain service for the Banking App. It owns account lifecycle and balance operations.

## Responsibilities

- Create and fetch accounts
- Return account balance and account history by user
- Update account status and close accounts
- Handle internal debit/credit/account lookup requests from other services

## Default Port

- `8082`

## Key Configuration

From `src/main/resources/application.properties`:

- `ACCOUNT_DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_API_TOKEN`

## API Endpoints

Base path: `/api/accounts`

Public via gateway/auth rules:

- `POST /api/accounts`
- `GET /api/accounts/{accountNumber}`
- `GET /api/accounts/user/{userId}`
- `GET /api/accounts/{accountNumber}/balance`
- `PUT /api/accounts/{accountNumber}/status?status=...`
- `DELETE /api/accounts/{accountNumber}`

Internal service-to-service (requires header `X-Internal-Token`):

- `POST /api/accounts/internal/debit`
- `POST /api/accounts/internal/credit`
- `GET /api/accounts/internal/{accountNumber}`

Invalid internal tokens return `403 Forbidden`.

## Run Locally

```bash
mvn -pl account-service -am spring-boot:run
```

## Tests

```bash
mvn -B -pl account-service -am test -f pom.xml
```
