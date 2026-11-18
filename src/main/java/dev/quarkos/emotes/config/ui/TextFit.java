package dev.quarkos.emotes.config.ui;

import java.util.function.ToIntFunction;

public final class TextFit {
    private static final String ELLIPSIS = "…";

    private TextFit() {}

    public static String ellipsize(String text, int limit, ToIntFunction<String> width) {
        if (limit <= 0) return "";
        if (width.applyAsInt(text) <= limit) return text;
        if (width.applyAsInt(ELLIPSIS) > limit) return "";

        int end = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (width.applyAsInt(text.substring(0, next) + ELLIPSIS) > limit) break;
            end = next;
        }
        return text.substring(0, end) + ELLIPSIS;
    }
}
