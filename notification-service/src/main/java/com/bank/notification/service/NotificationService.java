package com.bank.notification.service;

import java.lang.reflect.Method;

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
        String toEmail = extractStringField(event, "toEmail");
        String txId = event.getTransactionId();

        if (mailSender != null && toEmail != null) {
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
        } else if (toEmail == null) {
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

    private String extractStringField(TransactionEvent event, String fieldName) {
        try {
            Method m = event.getClass().getMethod("get" + capitalize(fieldName));
            Object v = m.invoke(event);
            return v != null ? v.toString() : null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            log.debug("Error extracting field {} from event: {}", fieldName, e.getMessage());
            return null;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
