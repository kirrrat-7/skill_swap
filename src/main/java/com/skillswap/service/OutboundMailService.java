package com.skillswap.service;

public interface OutboundMailService {
    void send(String recipient, String subject, String text);
}
