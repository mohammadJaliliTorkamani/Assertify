package org.example;

import java.util.regex.Pattern;

public class Assertion {
    private final String type;
    private final String content;

    public Assertion(String content) {
        this.type = Pattern.compile("assert\\s+", Pattern.CASE_INSENSITIVE).matcher(content).find() ? "java" : "junit";
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }
}
