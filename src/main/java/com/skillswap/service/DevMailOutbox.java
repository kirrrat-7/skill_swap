package com.skillswap.service;

import java.time.Instant;

public record DevMailOutbox(Instant sentAt, String recipient, String subject, String text) {
}
