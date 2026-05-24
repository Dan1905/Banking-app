package com.bank.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.BalanceResponse;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.UpdateBalanceRequest;
import com.bank.account.entity.Account;
import com.bank.account.entity.AccountStatus;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.InsufficientFundsException;
import com.bank.account.exception.InvalidAccountOperationException;
import com.bank.account.repository.AccountRepository;

import jakarta.transaction.Transactional;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountHolderName(account.getAccountHolderName())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
    
    private String generateAccountNumber() {
        String number;
        do {
            number = "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }


    private Account findActiveAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                    "Account not found: " + accountNumber
                ));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException(
                "Account is not active: " + accountNumber
            );
        }

        return account;
    }

    
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request){
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(request.getUserId())
                .accountHolderName(request.getAccountHolderName())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        return mapToResponse(accountRepository.save(account));
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        return mapToResponse(findActiveAccount(accountNumber));
    }

    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BalanceResponse getBalance(String accountNumber) {
        Account account = findActiveAccount(accountNumber);
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .build();
    }

    @Transactional
    public void debit(UpdateBalanceRequest request) {
        Account account = findActiveAccount(request.getAccountNumber());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds in account: " + request.getAccountNumber()
            );
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);
    }

    @Transactional
    public void credit(UpdateBalanceRequest request) {
        Account account = findActiveAccount(request.getAccountNumber());
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                    "Account not found: " + accountNumber
                ));

        account.setStatus(newStatus);
        return mapToResponse(accountRepository.save(account));
    }

    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = findActiveAccount(accountNumber);

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidAccountOperationException(
                "Cannot close account with non-zero balance: " + accountNumber
            );
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

}
