package com.bank.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.account.entity.AccountStatus;
import com.bank.account.entity.AccountType;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private Long userId;
    private String accountHolderName;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
