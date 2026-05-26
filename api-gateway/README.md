# API Gateway

Entry-point service for routing and JWT enforcement.

## Responsibilities

- Route incoming API traffic to backend services
- Apply JWT validation filter for protected routes
- Allow public auth endpoints based on filter logic

## Default Port

- `8080`

## Key Configuration

From `src/main/resources/application.properties`:

- `JWT_SECRET`
- `AUTH_SERVICE_URL`
- `ACCOUNT_SERVICE_URL`
- `TRANSACTION_SERVICE_URL`
- `NOTIFICATION_SERVICE_URL`

## Routes

Configured in `GatewayConfig`:

- `/api/auth/**` -> auth-service
- `/api/accounts/**` -> account-service
- `/api/transactions/**` -> transaction-service

JWT filter: `com.bank.api_gateway.filter.JwtAuthFilter`

## Run Locally

```bash
mvn -pl api-gateway -am spring-boot:run
```

## Tests

```bash
mvn -B -pl api-gateway -am test -f pom.xml
```

Run module integration tests:

```bash
mvn -B -pl api-gateway -am -Pintegration verify -f pom.xml
```
