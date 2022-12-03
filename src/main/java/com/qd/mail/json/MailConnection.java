package com.qd.mail.json;

public record MailConnection(String hostName, String hostPort, String username, String password, String protocol) {
}
