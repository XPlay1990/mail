package com.qd.mail.json;

import com.qd.mail.json.util.FilenetConfig;
import com.qd.mail.json.util.MailConnection;

public record MailConfig(String name, MailConnection mailConnection, String handler, String folder,
                         FilenetConfig filenetConfig) {
}