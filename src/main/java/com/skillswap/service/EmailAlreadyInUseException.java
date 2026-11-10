package com.skillswap.service;

public class EmailAlreadyInUseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmailAlreadyInUseException() {
        super("We couldn't create an account with those details. If you already registered, log in or reset your password.");
    }
}
