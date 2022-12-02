package com.qd.mail.service;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class MailFetcher {

    @Value("${spring.mail.username}")
    private String USERNAME;
    @Value("${spring.mail.password}")
    private String PASSWORD;

    @Value("${receiver.mail}")
    private String RECEIVER_MAIL;
    private static final String PROTOCOL = "imap";
    private static final String HOST = "imap.gmail.com";

    public MailFetcher(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    private final Authenticator auth = new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(USERNAME, PASSWORD);
        }
    };

    private Properties getProperties() {
        Properties props = new Properties();
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        props.put("mail.imap.socketFactory.port", "993");
        props.put("mail.imap.port", "993");
        props.put("mail.imap.host", HOST);
        props.put("mail.imap.user", USERNAME);
        props.put("mail.imap.protocol", PROTOCOL);
        return props;
    }

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final JavaMailSender emailSender;

    //    @Scheduled(fixedRate = 5000)
    public void sendMail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(USERNAME);
        message.setTo(RECEIVER_MAIL);
        message.setSubject("TEST");
        message.setText("TEST");
        emailSender.send(message);
    }

    @Scheduled(fixedRate = 10000)
    public void fetchMails() throws MessagingException, IOException {
        logger.info("Fetching Mails");

        // Creating mail session.
        Session session = Session.getDefaultInstance(getProperties(), auth);

        // Get the store provider and connect to the store.
        Store store = session.getStore(PROTOCOL);
        store.connect(HOST, USERNAME, PASSWORD);

        // Get folder and open the INBOX folder in the store.
        Folder inbox = store.getFolder("Test_1");
        inbox.open(Folder.READ_WRITE);
        FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

        // Retrieve the messages from the folder.
        Message[] messages = inbox.search(ft);
        if (messages.length == 0) {
            logger.info("No new Mails found.");
            return;
        }

        for (Message message : messages) {
            message.setFlag(Flags.Flag.SEEN, true);
            logger.info("Read email subject '" + message.getSubject());

            Object messageContent = message.getContent();
            if (messageContent instanceof String) return;

            if (messageContent instanceof Multipart multipart) {
                for (int bodyPartIndex = 0; bodyPartIndex < multipart.getCount(); bodyPartIndex++) {
                    BodyPart bodyPart = multipart.getBodyPart(bodyPartIndex);
                    Object bodyPartContent = bodyPart.getContent();

                    if (bodyPartContent instanceof InputStream || bodyPartContent instanceof String) {
                        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || StringUtils.isNotBlank(bodyPart.getFileName())) {
                            logger.info("Found Attachment: " + bodyPart.getFileName());
                        }
                    }
                }
            }
        }

        // Close folder and close store.
        inbox.close(true);
        store.close();
    }
}