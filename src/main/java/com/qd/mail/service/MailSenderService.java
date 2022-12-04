package com.qd.mail.service;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class MailSenderService {
    @Value("${spring.mail.username}")
    private String USERNAME;

    @Value("${receiver.mail}")
    private String RECEIVER_MAIL;

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final Lorem lorem = LoremIpsum.getInstance();

    public void sendSimpleMail() {
        log.info("Sending Mail...");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(USERNAME);
        message.setTo(RECEIVER_MAIL);
        message.setSubject(lorem.getTitle(10));
        message.setText(lorem.getParagraphs(20, 20));
        emailSender.send(message);
    }

    public void sendErrorMail(String id, String stacktrace) {
        try {
            HashMap<String, Object> params = new HashMap<>();

            params.put("id", id);
            params.put("stacktrace", stacktrace);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            Context context = new Context();
            context.setVariables(params);
            helper.setFrom(USERNAME);
            helper.setTo(RECEIVER_MAIL);
            helper.setSubject("Error while processing Mail " + id);
            String html = templateEngine.process("Error", context);
            helper.setText(html, true);

            log.info("Sending Error Mail");
            emailSender.send(message);
        } catch (Exception e) {
            log.error("Error while sending Error Mail. " + e.getMessage());
        }
    }
}