package com.bank.notification.service;

import org.springframework.stereotype.Service;

import com.bank.notification.kafka.TransactionEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {
    
    public void processTransactionEvent(TransactionEvent event) {
        switch (event.getType()) {
            case "TRANSFER" -> handleTransfer(event);
            case "DEPOSIT"  -> handleDeposit(event);
            case "WITHDRAWAL" -> handleWithdrawal(event);
            default -> log.warn("Unknown transaction type: {}", event.getType());
        }
    }

    private void handleTransfer(TransactionEvent event) {
        // TODO: plug in JavaMailSender or Twilio here
        log.info("TRANSFER notification — from: {} to: {} amount: {}",
                event.getFromAccountNumber(),
                event.getToAccountNumber(),
                event.getAmount());
    }

    private void handleDeposit(TransactionEvent event) {
        log.info("DEPOSIT notification — account: {} amount: {}",
                event.getToAccountNumber(),
                event.getAmount());
    }

    private void handleWithdrawal(TransactionEvent event) {
        log.info("WITHDRAWAL notification — account: {} amount: {}",
                event.getFromAccountNumber(),
                event.getAmount());
    }
}
