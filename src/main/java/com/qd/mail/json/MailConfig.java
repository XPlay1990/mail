package com.qd.mail.json;

public record MailConfig(String name, MailConnection mailConnection, String handler, String folder) {
}