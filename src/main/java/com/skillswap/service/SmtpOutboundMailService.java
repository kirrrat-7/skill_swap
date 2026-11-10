package com.skillswap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("!dev & !test")
public class SmtpOutboundMailService implements OutboundMailService {
    private final JavaMailSender sender;
    private final String from;

    public SmtpOutboundMailService(JavaMailSender sender,
                                   @Value("${skillswap.mail.from:}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String recipient, String subject, String text) {
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Mail delivery is not configured. Set SKILLSWAP_MAIL_FROM and SMTP environment variables.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        sender.send(message);
    }
}
