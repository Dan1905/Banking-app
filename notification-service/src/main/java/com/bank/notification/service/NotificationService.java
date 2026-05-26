package com.bank.notification.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.bank.notification.kafka.TransactionEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String EMAIL_TEMPLATE = "Dear Customer, your %s of $%.2f has been processed successfully. Transaction ID: %s.";

    // Mail sender is optional for local/dev. Use ObjectProvider to avoid requiring a bean.
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public void processTransactionEvent(TransactionEvent event) {
        if (event == null) {
            log.warn("Received null transaction event");
            return;
        }

        if (event.getType() == null) {
            log.warn("Unknown transaction type: null");
            return;
        }

        switch (event.getType()) {
            case TRANSFER -> handleTransfer(event);
            case DEPOSIT -> handleDeposit(event);
            case WITHDRAWAL -> handleWithdrawal(event);
            default -> log.warn("Unknown transaction type: {}", event.getType());
        }
    }

    private void handleTransfer(TransactionEvent event) {
        // Attempt to send email only if a JavaMailSender bean is available and the event contains an email.
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        String toEmail = event.getEmail();
        String txId = event.getTransactionId();
        boolean hasEmail = toEmail != null && !toEmail.isBlank();

        if (mailSender != null && hasEmail) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("Transaction Notification");
                message.setText(String.format(EMAIL_TEMPLATE, "transfer", event.getAmount(), txId));
                mailSender.send(message);
                log.info("Sent TRANSFER email to {} for transaction {}", toEmail, txId);
            } catch (Exception ex) {
                log.error("Failed to send TRANSFER email for transaction {}", txId, ex);
            }
        } else if (!hasEmail) {
            log.info("No email address available on event {}; skipping send", txId);
        } else {
            log.info("No JavaMailSender configured; skipping email for transaction {}", txId);
        }

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
