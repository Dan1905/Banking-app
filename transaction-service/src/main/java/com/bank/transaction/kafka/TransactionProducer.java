package com.bank.transaction.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProducer {
     private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${kafka.topic.transaction-events}")
    private String topic;

    public void publishTransactionEvent(TransactionEvent event) {
        kafkaTemplate.send(topic, event.getTransactionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event: {}", event.getTransactionId(), ex);
                    } else {
                        log.info("Published transaction event: {}", event.getTransactionId());
                    }
                });
    }
}
