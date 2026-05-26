# Banking App

A Spring Boot microservices banking platform built as a single monorepo. It combines REST services, JWT-based authentication, Kafka event streaming, and MySQL-backed persistence.

This repository is meant to be read on GitHub by developers, reviewers, and anyone trying to understand how the system is split into services and how the pieces talk to each other.

## What this project contains

The system is divided into five Spring Boot services:

- `auth-service` handles registration, login, and JWT generation.
- `account-service` manages bank accounts, balances, and account state.
- `transaction-service` handles deposits, withdrawals, and transfers, then publishes transaction events to Kafka.
- `notification-service` consumes Kafka transaction events and can trigger downstream notifications.
- `api-gateway` sits in front of the services and validates JWT-protected requests.

Supporting infrastructure is provided with Docker Compose:

- MySQL for persistence
- Kafka for async event delivery
- Zookeeper for Kafka coordination

When using `docker compose up -d`, MySQL is exposed on `localhost:3307` to avoid conflicts with any local MySQL server already using `3306`.

## Environment variables

Use [`.env.example`](.env.example) as the template for local development.

- Copy it to `.env` on your machine and fill in real values.
- Keep `.env` out of GitHub.
- For cloud deployment, set these values in your hosting platform's environment or secret settings instead of committing them.

The current Spring Boot services already read their settings from `application.properties`, so the env file is mainly a safe template for local overrides and deployment secrets.

## Architecture at a glance

```text
Client
  |
  v
API Gateway
  |
  +--> auth-service ---------> MySQL auth_db
  |
  +--> account-service ------> MySQL account_db
  |
  +--> transaction-service --> MySQL transaction_db
  |                               |
  |                               v
  |                            Kafka topic(s)
  |                               |
  v                               v
notification-service <----------- Kafka consumer
```

### Request flow

1. A client sends a request to the `api-gateway`.
2. If the route is public, the gateway forwards it.
3. If the route is protected, the gateway validates the JWT.
4. The request reaches the target service.
5. The service uses its own database and returns a response.
6. For transaction events, `transaction-service` also publishes a Kafka message.
7. `notification-service` consumes the message asynchronously.

## Module breakdown

### `auth-service`

Responsible for identity and token creation.

Service README: [auth-service/README.md](auth-service/README.md)

Key responsibilities:

- register new users
- authenticate existing users
- issue JWTs
- expose auth-related endpoints

Important code areas:

- application bootstrap: [auth-service/src/main/java/com/bank/auth/AuthApplication.java](auth-service/src/main/java/com/bank/auth/AuthApplication.java)
- security configuration: [auth-service/src/main/java/com/bank/auth/config/SecurityConfig.java](auth-service/src/main/java/com/bank/auth/config/SecurityConfig.java)
- auth controller: [auth-service/src/main/java/com/bank/auth/controller/AuthController.java](auth-service/src/main/java/com/bank/auth/controller/AuthController.java)
- DTOs: [auth-service/src/main/java/com/bank/auth/dto](auth-service/src/main/java/com/bank/auth/dto)
- user model: [auth-service/src/main/java/com/bank/auth/entity](auth-service/src/main/java/com/bank/auth/entity)

### `account-service`

Responsible for account lifecycle and balance changes.

Service README: [account-service/README.md](account-service/README.md)

Key responsibilities:

- create accounts
- update account balances
- fetch balances and account details
- reject invalid operations such as insufficient funds or invalid state transitions

Important code areas:

- application bootstrap: [account-service/src/main/java/com/bank/account/AccountApplication.java](account-service/src/main/java/com/bank/account/AccountApplication.java)
- account controller: [account-service/src/main/java/com/bank/account/controller/AccountController.java](account-service/src/main/java/com/bank/account/controller/AccountController.java)
- business logic: [account-service/src/main/java/com/bank/account/service/AccountService.java](account-service/src/main/java/com/bank/account/service/AccountService.java)
- security: [account-service/src/main/java/com/bank/account/config](account-service/src/main/java/com/bank/account/config)
- domain model: [account-service/src/main/java/com/bank/account/entity](account-service/src/main/java/com/bank/account/entity)

### `transaction-service`

Responsible for transaction processing and event publication.

Service README: [transaction-service/README.md](transaction-service/README.md)

Key responsibilities:

- process deposits, withdrawals, and transfers
- persist transactions
- publish transaction events to Kafka
- expose transaction APIs

Important code areas:

- application bootstrap: [transaction-service/src/main/java/com/bank/transaction/TransactionApplication.java](transaction-service/src/main/java/com/bank/transaction/TransactionApplication.java)
- transaction controller: [transaction-service/src/main/java/com/bank/transaction/controller/TransactionController.java](transaction-service/src/main/java/com/bank/transaction/controller/TransactionController.java)
- business logic: [transaction-service/src/main/java/com/bank/transaction/service/TransactionService.java](transaction-service/src/main/java/com/bank/transaction/service/TransactionService.java)
- Kafka producer: [transaction-service/src/main/java/com/bank/transaction/kafka/TransactionProducer.java](transaction-service/src/main/java/com/bank/transaction/kafka/TransactionProducer.java)
- Kafka config: [transaction-service/src/main/java/com/bank/transaction/config/KafkaProducerConfig.java](transaction-service/src/main/java/com/bank/transaction/config/KafkaProducerConfig.java)

### `notification-service`

Responsible for consuming transaction events and reacting to them.

Service README: [notification-service/README.md](notification-service/README.md)

Key responsibilities:

- listen for Kafka transaction events
- transform events into notification actions
- keep asynchronous side effects outside the transaction flow

Important code areas:

- application bootstrap: [notification-service/src/main/java/com/bank/notification/NotificationApplication.java](notification-service/src/main/java/com/bank/notification/NotificationApplication.java)
- Kafka consumer: [notification-service/src/main/java/com/bank/notification/kafka/TransactionConsumer.java](notification-service/src/main/java/com/bank/notification/kafka/TransactionConsumer.java)
- notification logic: [notification-service/src/main/java/com/bank/notification/service/NotificationService.java](notification-service/src/main/java/com/bank/notification/service/NotificationService.java)
- Kafka config: [notification-service/src/main/java/com/bank/notification/config/KafkaConsumerConfig.java](notification-service/src/main/java/com/bank/notification/config/KafkaConsumerConfig.java)

### `api-gateway`

Responsible for central request routing and JWT protection.

Service README: [api-gateway/README.md](api-gateway/README.md)

Key responsibilities:

- route requests to backend services
- allow public auth endpoints
- block internal/private endpoints from direct access
- validate JWT on protected routes

Important code areas:

- application bootstrap: [api-gateway/src/main/java/com/bank/api_gateway/ApiGatewayApplication.java](api-gateway/src/main/java/com/bank/api_gateway/ApiGatewayApplication.java)
- routing config: [api-gateway/src/main/java/com/bank/api_gateway/config/GatewayConfig.java](api-gateway/src/main/java/com/bank/api_gateway/config/GatewayConfig.java)
- JWT filter: [api-gateway/src/main/java/com/bank/api_gateway/filter/JwtAuthFilter.java](api-gateway/src/main/java/com/bank/api_gateway/filter/JwtAuthFilter.java)
- gateway exception: [api-gateway/src/main/java/com/bank/api_gateway/filter/GatewayAuthException.java](api-gateway/src/main/java/com/bank/api_gateway/filter/GatewayAuthException.java)

## Data ownership

Each service owns its own database. This avoids tight coupling and lets each service evolve independently.

| Service | Database |
| --- | --- |
| `auth-service` | `auth_db` |
| `account-service` | `account_db` |
| `transaction-service` | `transaction_db` |

The database names are created by [init-db.sql](init-db.sql).

## Event flow

Transaction events are designed to flow through Kafka:

1. A transaction is created in `transaction-service`.
2. The transaction is saved in MySQL.
3. Account ownership is resolved via internal lookups and event email metadata is enriched when available.
4. A `TransactionEvent` is published to Kafka after the DB transaction commits.
4. `notification-service` consumes the event.
5. The notification layer can send email, SMS, audit logs, or other side effects.

This keeps the core transaction path fast and isolates downstream work from the request lifecycle.

## Local development

### Prerequisites

- Java 17
- Maven 3.9+
- Docker and Docker Compose

### Start infrastructure

```bash
docker compose up -d
```

This starts:

- MySQL on `localhost:3307` when using Docker Compose
- Kafka on `localhost:29092` for host clients (`kafka:9092` for containers)
- Zookeeper on `localhost:2181`

### Run services

Run each service separately from its module folder or from the repository root.

Example:

```bash
mvn -pl auth-service -am spring-boot:run
```

```bash
mvn -pl account-service -am spring-boot:run
```

```bash
mvn -pl transaction-service -am spring-boot:run
```

```bash
mvn -pl notification-service -am spring-boot:run
```

```bash
mvn -pl api-gateway -am spring-boot:run
```

### Build and test

Compile and package everything:

```bash
mvn -T1C -DskipTests package
```

Run all tests:

```bash
mvn -T1C test
```

Run full verification (unit + integration profiles configured in the repo):

```bash
mvn -B verify -f pom.xml
```

Run tests for a single service:

```bash
mvn -pl auth-service -am test
```

## Swagger / API docs

Swagger UI is enabled on the REST services so you can try endpoints in the browser.

Open these after starting the services:

- Auth service: http://localhost:8081/swagger-ui.html
- Account service: http://localhost:8082/swagger-ui.html
- Transaction service: http://localhost:8083/swagger-ui.html

If you run the services in Docker, make sure those ports are exposed in your Compose file or access the docs through a reverse proxy.

Swagger is useful for quick manual testing while you are still learning the flow, but keep the automated tests as the real source of confidence.

## Configuration notes

### Kafka

The compose file advertises Kafka for local host clients on `PLAINTEXT_HOST://localhost:29092`.

- If you run services on your machine, use `localhost:29092`.
- If you run services in Docker on the same network, use `kafka:9092`.

### Internal service token

Internal service-to-service endpoints use the `X-Internal-Token` header.

- `transaction-service` calls internal endpoints on `account-service` and `auth-service`.
- Services validate the token against `internal.api-token`.
- Invalid internal tokens return `403 Forbidden`.

### MySQL

The database init script creates the service databases the first time the MySQL volume is created.

If you need to re-run initialization, remove the MySQL volume first or create the databases manually.

## Error handling

Each service has a dedicated global exception handler that returns structured error responses.

- `account-service`: [account-service/src/main/java/com/bank/account/exception/GlobalExceptionHandler.java](account-service/src/main/java/com/bank/account/exception/GlobalExceptionHandler.java)
- `transaction-service`: [transaction-service/src/main/java/com/bank/transaction/exception/GlobalExceptionHandler.java](transaction-service/src/main/java/com/bank/transaction/exception/GlobalExceptionHandler.java)
- `api-gateway`: [api-gateway/src/main/java/com/bank/api_gateway/exception/GlobalExceptionHandler.java](api-gateway/src/main/java/com/bank/api_gateway/exception/GlobalExceptionHandler.java)

The handlers typically return:

- HTTP status
- error reason phrase
- user-friendly message
- timestamp
- validation field errors when applicable

## Repository layout

```text
banking-app/
├── api-gateway/
├── account-service/
├── auth-service/
├── notification-service/
├── transaction-service/
├── docker-compose.yml
├── init-db.sql
└── pom.xml
```

## Current implementation details worth knowing

- JWT handling is centralized in the gateway and auth service.
- Kafka is used for asynchronous event delivery, not for core transaction writes.
- Each service has its own Spring Boot application and its own test folder.
- The codebase is organized by feature: `controller`, `service`, `repository`, `entity`, `dto`, `config`, and `exception`.

## Troubleshooting

- If the gateway starts but protected endpoints fail, verify the JWT secret and the token format.
- If Kafka consumers do not receive events, confirm the broker is running and the bootstrap server matches the environment.
- If MySQL starts with empty databases, check `init-db.sql` and the Docker volume state.
- If a service fails to start, look at its `application.properties` and module-specific logs.


