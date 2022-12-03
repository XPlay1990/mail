package com.qd.mail.service;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MailSenderService {
    @Value("${spring.mail.username}")
    private String USERNAME;

    @Value("${receiver.mail}")
    private String RECEIVER_MAIL;

    private final JavaMailSender emailSender;

    private final Lorem lorem;

    public MailSenderService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
        this.lorem = LoremIpsum.getInstance();

    }

    @Async
//    @Scheduled(fixedRate = 500)
    public void sendMail() {
        log.info("Sending Mail...");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(USERNAME);
        message.setTo(RECEIVER_MAIL);
        message.setSubject(lorem.getTitle(10));
        message.setText(lorem.getParagraphs(20, 20));
        emailSender.send(message);
    }
}