package com.qd.mail.mailHandler;

import com.qd.mail.json.MailConfig;
import jakarta.mail.Message;

import java.util.concurrent.CompletableFuture;

public interface AbstractMailHandler {
    String name = "HandlerName";
    String description = "HandlerDescription";

    CompletableFuture<String> handleMessage(Message message, MailConfig mailConfig);

    String getName();

    String getDescription();
}
