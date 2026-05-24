package com.bank.transaction.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.transaction.entity.TransactionType;

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
    private TransactionType type;
    private String status;
    private LocalDateTime createdAt;
}
