package com.qd.mail.mailHandler;

import com.qd.mail.json.MailConfig;
import com.qd.mail.service.FileNetP8Service;
import com.qd.mail.service.MailSenderService;
import jakarta.mail.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@RequiredArgsConstructor
@Component
public class SimpleMailHandler implements AbstractMailHandler {
    private final String name = "SimpleMailHandler";
    private final String description = "SimpleMailHandler";

    private final MailSenderService mailSenderService;
    private final FileNetP8Service fileNetP8Service;

    @Async
    @Override
    public CompletableFuture<String> handleMessage(Message message, MailConfig mailConfig) {
        CompletableFuture<String> isComplete = new CompletableFuture<>();
        try {
            log.info("Processing message: " + message.getSubject());

            HashMap<String, Object> mailProperties = new HashMap<>();
            mailProperties.put("SUBJECT", message.getSubject());
            mailProperties.put("FROM", message.getFrom());
            mailProperties.put("TO", message.getAllRecipients());
            mailProperties.put("SENT_DATE", message.getSentDate());
            mailProperties.put("RECEIVED_DATE", message.getReceivedDate());

            Object messageContent = message.getContent();

            if (messageContent instanceof Multipart multipart) {
                for (int bodyPartIndex = 0; bodyPartIndex < multipart.getCount(); bodyPartIndex++) {
                    BodyPart bodyPart = multipart.getBodyPart(bodyPartIndex);
                    Object bodyPartContent = bodyPart.getContent();

                    if (bodyPartContent instanceof InputStream || bodyPartContent instanceof String) {
                        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || StringUtils.isNotBlank(bodyPart.getFileName())) {
                            log.info("Found Attachment: " + bodyPart.getFileName());
                            fileNetP8Service.createFilenetDocument(mailConfig, bodyPart, mailProperties);
                        }
                    }
                }
            }

            message.setFlag(Flags.Flag.SEEN, true);
            log.info("Message processed");
        } catch (Exception e) {
            try {
                log.error("Error while processing Message with subject " + message.getSubject() + " in MailConfig " + mailConfig.name() + ". " + e.getMessage());
                mailSenderService.sendErrorMail(message.getSubject(), ExceptionUtils.getStackTrace(e));
            } catch (MessagingException ex) {
                log.error("Error while processing Message with number " + message.getMessageNumber() + " in MailConfig " + mailConfig.name() + ". " + e.getMessage());
                mailSenderService.sendErrorMail(String.valueOf(message.getMessageNumber()), ExceptionUtils.getStackTrace(e));
            }
        }
        isComplete.complete("Message processed");
        return isComplete;
    }
}
