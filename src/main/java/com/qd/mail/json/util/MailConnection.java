package com.qd.mail.json.util;

public record MailConnection(String hostName, String hostPort, String username, String password, String protocol) {
}
