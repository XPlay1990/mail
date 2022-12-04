package com.qd.mail.json.util;

import java.util.HashMap;

public record FilenetConfig(String documentClassName, String osName, String targetFolder, HashMap<String, String> propertiesMapping) {
}
