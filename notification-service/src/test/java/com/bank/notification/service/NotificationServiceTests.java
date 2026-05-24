package com.bank.notification.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bank.notification.kafka.TransactionEvent;

class NotificationServiceTests {

    private final NotificationService notificationService = new NotificationService();

    @Test
    @DisplayName("processTransactionEvent should handle transfer events")
    void testProcessTransferEvent() {
        TransactionEvent event = buildEvent("TRANSFER");
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should handle deposit events")
    void testProcessDepositEvent() {
        TransactionEvent event = buildEvent("DEPOSIT");
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should handle withdrawal events")
    void testProcessWithdrawalEvent() {
        TransactionEvent event = buildEvent("WITHDRAWAL");
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should ignore unknown event types safely")
    void testProcessUnknownEventType() {
        TransactionEvent event = buildEvent("UNKNOWN");
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    private TransactionEvent buildEvent(String type) {
        return new TransactionEvent(
                "txn-123",
                "ACC100",
                "ACC200",
                new BigDecimal("50.00"),
                type,
                "SUCCESS",
                LocalDateTime.now()
        );
    }
}
