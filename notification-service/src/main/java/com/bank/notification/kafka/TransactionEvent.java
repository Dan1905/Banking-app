package com.bank.notification.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.notification.entity.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String email; // for notification service
    private TransactionType type;
    private String status;
    private LocalDateTime createdAt;
}
