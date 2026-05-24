package com.bank.transaction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByFromAccountNumber(String accountNumber);
    List<Transaction> findByToAccountNumber(String accountNumber);
    List<Transaction> findByFromAccountNumberOrToAccountNumber(String from, String to);
    List<Transaction> findByStatus(TransactionStatus status);
}
