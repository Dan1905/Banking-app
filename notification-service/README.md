# Notification Service

Kafka consumer service that reacts to transaction events.

## Responsibilities

- Consume `TransactionEvent` from Kafka topic `transaction.events`
- Process transfer/deposit/withdrawal notifications
- Send transfer email when `JavaMailSender` is configured and event email exists
- Skip email gracefully when sender is not configured or email is blank

## Default Port

- `8084`

## Key Configuration

From `src/main/resources/application.properties`:

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `spring.kafka.consumer.group-id` (default `notification-group`)
- `KAFKA_TOPIC_TRANSACTION_EVENTS`

## Event Consumption

Consumer class: `com.bank.notification.kafka.TransactionConsumer`

- Topic: `${kafka.topic.transaction-events}`
- Group: `${spring.kafka.consumer.group-id}`

## Run Locally

```bash
mvn -pl notification-service -am spring-boot:run
```

## Tests

```bash
mvn -B -pl notification-service -am test -f pom.xml
```
