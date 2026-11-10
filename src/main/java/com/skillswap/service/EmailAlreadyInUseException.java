package com.skillswap.service;

public class EmailAlreadyInUseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EmailAlreadyInUseException() {
        super("An account with this email address already exists. Try logging in instead.");
    }
}
