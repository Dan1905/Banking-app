package com.bank.notification.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import com.bank.notification.entity.TransactionType;
import com.bank.notification.kafka.TransactionEvent;

class NotificationServiceTests {

    private final ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
    private final NotificationService notificationService = new NotificationService(mailSenderProvider);

    NotificationServiceTests() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    @DisplayName("processTransactionEvent should handle transfer events")
    void testProcessTransferEvent() {
        TransactionEvent event = buildEvent(TransactionType.TRANSFER);
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should handle deposit events")
    void testProcessDepositEvent() {
        TransactionEvent event = buildEvent(TransactionType.DEPOSIT);
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should handle withdrawal events")
    void testProcessWithdrawalEvent() {
        TransactionEvent event = buildEvent(TransactionType.WITHDRAWAL);
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    @Test
    @DisplayName("processTransactionEvent should ignore null event type safely")
    void testProcessNullEventType() {
        TransactionEvent event = buildEvent(null);
        assertDoesNotThrow(() -> notificationService.processTransactionEvent(event));
    }

    private TransactionEvent buildEvent(TransactionType type) {
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
