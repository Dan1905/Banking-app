package com.bank.notification.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bank.notification.service.NotificationService;
import com.bank.common.kafka.TransactionEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topic.transaction-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(TransactionEvent event) {
        log.info("Consumed transaction event: {}", event.getTransactionId());
        try {
            notificationService.processTransactionEvent(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getTransactionId(), e);
        }
    }
}
