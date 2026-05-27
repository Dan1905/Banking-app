package com.bank.transaction.dto;

import java.math.BigDecimal;

import com.bank.common.model.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DepositWithdrawalRequest {
    @NotBlank
    private String accountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;   // DEPOSIT or WITHDRAWAL only

    private String description;
}
