package com.qd.mail.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.qd.mail.json.MailConfig;
import com.qd.mail.json.util.MailConnection;
import com.qd.mail.json.MailConnectionList;
import com.qd.mail.mailHandler.AbstractMailHandler;
import com.qd.mail.service.util.MailAuthenticator;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class MailFetcherService {
    private final List<AbstractMailHandler> mailHandlerList;

    public MailFetcherService(List<AbstractMailHandler> mailHandlerList) {
        this.mailHandlerList = mailHandlerList;
    }

    @Scheduled(fixedDelay = 10000)
    public void fetchConfiguredMails() throws FileNotFoundException {
        AtomicReference<Integer> processedMailCount = new AtomicReference<>(0);

        log.info("Reading MailConfig...");
        List<MailConfig> mailConfigList = readMailConfig();

        log.info("Fetching Mails...");
        mailConfigList.forEach(mailConfig -> {
            MailConnection mailConnection = mailConfig.mailConnection();
            Session session = Session.getDefaultInstance(getProperties(mailConnection.hostName(), mailConnection.hostPort(), mailConnection.protocol()), new MailAuthenticator(mailConnection.username(), mailConnection.password()));

            try {
                // Get the store provider and connect to the store.
                Store store = session.getStore(mailConnection.protocol());
                store.connect(mailConnection.hostName(), mailConnection.username(), mailConnection.password());

                // Get folder and open the INBOX folder in the store.
                Folder inbox = store.getFolder(mailConfig.folder());
                inbox.open(Folder.READ_WRITE);
                FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

                // Retrieve the messages from the folder.
                Message[] messages = inbox.search(ft);
                processedMailCount.updateAndGet(v -> v + messages.length);

                AbstractMailHandler mailHandler = findMailHandler(mailConfig);

                //Handle Messages
                List<CompletableFuture<String>> messageResults = new ArrayList<>();
                for (Message message : messages) {
                    messageResults.add(mailHandler.handleMessage(message, mailConfig));
                }
                CompletableFuture.allOf(messageResults.toArray(new CompletableFuture[0])).join();


                // Close folder and close store.
                inbox.close(true);
                store.close();
            } catch (MessagingException e) {
                log.error("Error while fetching Messages for  " + mailConfig.name() + ". " + e.getMessage());
            }
        });
        log.info("Processed " + processedMailCount + " Mails.");
    }


    private Properties getProperties(String host, String port, String protocol) {
        Properties props = new Properties();
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        props.put("mail.imap.socketFactory.port", port);
        props.put("mail.imap.port", port);
        props.put("mail.imap.host", host);
        props.put("mail.imap.protocol", protocol);
        return props;
    }

    private List<MailConfig> readMailConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader("./MailConfig.json"));
        return gson.fromJson(reader, MailConnectionList.class);
    }

    private AbstractMailHandler findMailHandler(MailConfig mailConfig) {
        String handlerName = mailConfig.handler();
        for (AbstractMailHandler mailHandler : mailHandlerList) {
            if (handlerName.equals(mailHandler.getName())) {
                return mailHandler;
            }
        }
        throw new RuntimeException("No Matching Handler found for " + handlerName + " in MailConfig " + mailConfig.name());
    }
}