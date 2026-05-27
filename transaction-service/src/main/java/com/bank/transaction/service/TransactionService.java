package com.bank.transaction.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bank.transaction.client.AccountClient;
import com.bank.transaction.client.AuthClient;
import com.bank.transaction.dto.AccountLookupResponse;
import com.bank.transaction.dto.DepositWithdrawalRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.dto.UpdateBalanceRequest;
import com.bank.transaction.dto.UserEmailResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.common.kafka.TransactionEvent;
import com.bank.transaction.kafka.TransactionProducer;
import com.bank.common.model.TransactionType;
import com.bank.transaction.repository.TransactionRepository;

import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final AuthClient authClient;
    private final TransactionProducer transactionProducer;

    @Value("${internal.api-token}")
    private String internalToken;

        // --- helpers ---

    private TransactionEvent mapToEvent(Transaction t) {
        return TransactionEvent.builder()
                .transactionId(t.getTransactionId())
                .fromAccountNumber(t.getFromAccountNumber())
                .toAccountNumber(t.getToAccountNumber())
                .amount(t.getAmount())
                .email(resolveEmail(t))
                .type(t.getType())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private String resolveEmail(Transaction t) {
        String accountNumber = t.getFromAccountNumber();
        if (accountNumber == null || accountNumber.isBlank()) {
            accountNumber = t.getToAccountNumber();
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }

        try {
            AccountLookupResponse account = accountClient.getAccountInternal(internalToken, accountNumber);
            if (account == null || account.getUserId() == null) {
                return null;
            }
            UserEmailResponse user = authClient.getUserEmail(internalToken, account.getUserId());
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return null;
            }
            return user.getEmail();
        } catch (Exception ex) {
            log.warn("Failed to resolve email for account {}", accountNumber, ex);
            return null;
        }
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                .fromAccountNumber(t.getFromAccountNumber())
                .toAccountNumber(t.getToAccountNumber())
                .amount(t.getAmount())
                .type(t.getType())
                .status(t.getStatus())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) throws InvalidTransactionException {
        if(request.getFromAccountNumber().equals(request.getToAccountNumber())){
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transactionRepository.save(transaction);

        try{
            //debit source credit desitnation
            UpdateBalanceRequest debitRequest = new UpdateBalanceRequest();
            debitRequest.setAccountNumber(request.getFromAccountNumber());
            debitRequest.setAmount(request.getAmount());
            accountClient.debit(internalToken, debitRequest);
            
            UpdateBalanceRequest creditRequest = new UpdateBalanceRequest();
            creditRequest.setAccountNumber(request.getToAccountNumber());
            creditRequest.setAmount(request.getAmount());
            accountClient.credit(internalToken, creditRequest);
            
            transaction.setStatus(TransactionStatus.SUCCESS);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("Error occurred while transferring funds", e);
            throw new InvalidTransactionException("Error occurred while transferring funds");
        }
        
        transactionRepository.save(transaction);
        final var evt = mapToEvent(transaction);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    transactionProducer.publishTransactionEvent(evt);
                }
            });
        } else {
            // No transaction synchronization available (e.g., unit tests). Publish immediately.
            transactionProducer.publishTransactionEvent(evt);
        }
        return mapToResponse(transaction);
    }

    @Transactional
    public TransactionResponse depositOrWithdraw(DepositWithdrawalRequest request) throws InvalidTransactionException {
        if (request.getType() == TransactionType.TRANSFER) {
            throw new InvalidTransactionException("Use /transfer endpoint for transfers");
        }

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
            .fromAccountNumber(request.getAccountNumber())
            .toAccountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .type(request.getType())
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transactionRepository.save(transaction);

        try {
            UpdateBalanceRequest updateRequest = new UpdateBalanceRequest();
            updateRequest.setAccountNumber(request.getAccountNumber());
            updateRequest.setAmount(request.getAmount());
            
            if (request.getType() == TransactionType.DEPOSIT) {
                accountClient.credit(internalToken, updateRequest);
            } else {
                accountClient.debit(internalToken, updateRequest);
            }
            transaction.setStatus(TransactionStatus.SUCCESS);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            log.error("Error occurred during deposit/withdrawal", e);
            throw new InvalidTransactionException("Transaction failed: " + e.getMessage());
        }

        transactionRepository.save(transaction);
        final var evt2 = mapToEvent(transaction);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    transactionProducer.publishTransactionEvent(evt2);
                }
            });
        } else {
            transactionProducer.publishTransactionEvent(evt2);
        }
        return mapToResponse(transaction);
    }
    
    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId)));
    }

    public List<TransactionResponse> getAccountHistory(String accountNumber) {
        return transactionRepository
                .findByFromAccountNumberOrToAccountNumber(accountNumber, accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}
