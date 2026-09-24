package com.gregtechceu.gtceu.utils;

/** Compile-only tooltip formatting helper; formatting is outside this selected-source check. */
public final class FormattingUtil {

    private FormattingUtil() {}

    public static String formatTime(long ticks) {
        return Long.toString(ticks);
    }
}
