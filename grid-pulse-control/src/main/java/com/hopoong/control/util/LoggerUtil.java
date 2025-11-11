package com.hopoong.control.util;

import org.slf4j.Logger;

public class LoggerUtil {

    private static final int LINE_WIDTH = 60;

    public static void block(Logger log, String title) {
        String border = "=".repeat(LINE_WIDTH);
        String centered = center(title, LINE_WIDTH);

        log.info("\n{}\n{}\n{}", border, centered, border);
    }

    public static void block(Logger log, String title, String content) {
        String border = "=".repeat(LINE_WIDTH);
        String centered = center(title, LINE_WIDTH);

        log.info("\n{}\n{}\n{}\n{}", border, centered, border, content);
    }

    public static void line(Logger log, String content) {
        log.info("[ {} ]", content);
    }

    public static void section(Logger log, String title) {
        String border = "-".repeat(LINE_WIDTH);
        log.info("\n{}\n{}{}\n{}", border, ">> ", title, border);
    }

    private static String center(String text, int width) {
        int padding = (width - text.length()) / 2;
        if (padding <= 0) return text;
        return " ".repeat(padding) + text;
    }

}
