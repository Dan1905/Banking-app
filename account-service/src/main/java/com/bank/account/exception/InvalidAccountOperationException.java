package com.bank.account.exception;

public class InvalidAccountOperationException extends RuntimeException{
    public InvalidAccountOperationException(String message) {
        super(message);
    }
}
