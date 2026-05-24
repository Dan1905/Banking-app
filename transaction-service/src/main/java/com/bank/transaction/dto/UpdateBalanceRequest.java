package com.bank.transaction.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateBalanceRequest {
     @NotBlank
    private String accountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;
}
