package com.skillswap.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "skillswap.mail.dev-outbox", havingValue = "true")
public class DevMailOutboxService implements OutboundMailService {
    private static final int LIMIT = 100;
    private final ConcurrentLinkedDeque<DevMailOutbox> messages = new ConcurrentLinkedDeque<>();

    @Override
    public void send(String recipient, String subject, String text) {
        messages.addFirst(new DevMailOutbox(Instant.now(), recipient, subject, text));
        while (messages.size() > LIMIT) messages.pollLast();
    }

    public List<DevMailOutbox> latest() { return List.copyOf(messages); }

    public void clear() { messages.clear(); }
}
