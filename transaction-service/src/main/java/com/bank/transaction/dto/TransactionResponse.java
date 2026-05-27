package com.bank.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.transaction.entity.TransactionStatus;
import com.bank.common.model.TransactionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponse {
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}
