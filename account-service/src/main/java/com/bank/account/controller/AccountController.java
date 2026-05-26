package com.bank.account.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.BalanceResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.UpdateBalanceRequest;
import com.bank.account.entity.AccountStatus;
import com.bank.account.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @Value("${internal.api-token}")
    private String internalApiToken;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN') or (hasRole('CUSTOMER') and #userId == authentication.principal)")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable String accountNumber,
            @RequestParam AccountStatus status) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, status));
    }

    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> closeAccount(@PathVariable String accountNumber) {
        accountService.closeAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    // Internal endpoints for transaction service
    private void validateInternalToken(String token) {
        if (!internalApiToken.equals(token)) {
            throw new SecurityException("Invalid internal token");
        }
    }
    @PostMapping("/internal/debit")
    public ResponseEntity<Void> debit(@RequestHeader("X-Internal-Token") String internalToken, @Valid @RequestBody UpdateBalanceRequest request){
        validateInternalToken(internalToken);
        accountService.debit(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/credit")
    public ResponseEntity<Void> credit(@RequestHeader("X-Internal-Token") String internalToken, @Valid @RequestBody UpdateBalanceRequest request){
        validateInternalToken(internalToken);
        accountService.credit(request);
        return ResponseEntity.ok().build(); 
    }

    @GetMapping("/internal/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountInternal(
            @RequestHeader("X-Internal-Token") String internalToken,
            @PathVariable String accountNumber) {
        validateInternalToken(internalToken);
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }
}
