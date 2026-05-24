package com.bank.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class UpdateBalanceRequest {
    @NotNull
    private String accountNumber;

    @NotNull
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
}
