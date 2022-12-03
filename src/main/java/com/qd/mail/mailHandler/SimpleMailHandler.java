package com.qd.mail.mailHandler;

import com.qd.mail.json.MailConfig;
import jakarta.mail.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
@Component
public class SimpleMailHandler implements AbstractMailHandler {
    private final String name = "SimpleMailHandler";
    private final String description = "SimpleMailHandler";

    @Async
    @Override
    public CompletableFuture<String> handleMessage(Message message, MailConfig mailConfig) {
        CompletableFuture<String> isComplete = new CompletableFuture<>();
        try {
            log.info("Processing message: " + message.getSubject());

            Object messageContent = message.getContent();
            if (messageContent instanceof String) {
                isComplete.complete("Message processed");
                return isComplete;
            }

            if (messageContent instanceof Multipart multipart) {
                for (int bodyPartIndex = 0; bodyPartIndex < multipart.getCount(); bodyPartIndex++) {
                    BodyPart bodyPart = multipart.getBodyPart(bodyPartIndex);
                    Object bodyPartContent = bodyPart.getContent();

                    if (bodyPartContent instanceof InputStream || bodyPartContent instanceof String) {
                        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || StringUtils.isNotBlank(bodyPart.getFileName())) {
                            log.info("Found Attachment: " + bodyPart.getFileName());
                        }
                    }
                }
            }

            message.setFlag(Flags.Flag.SEEN, true);
        } catch (Exception e) {
            try {
                log.error("Error while processing Message. " + ExceptionUtils.getStackTrace(e));
                log.error("Error while processing Message with subject " + message.getSubject() + " in MailConfig " + mailConfig.name() + ". " + e.getMessage());
            } catch (MessagingException ex) {
                log.error("Error while processing Message with number " + message.getMessageNumber() + " in MailConfig " + mailConfig.name() + ". " + e.getMessage());
            }
        }
        isComplete.complete("Message processed");
        return isComplete;
    }
}
