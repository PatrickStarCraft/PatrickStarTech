package com.gregtechceu.gtceu.utils;

/** Compile-only cast helper for the unexecuted getModifier path. */
public final class GTMath {
    private GTMath() {}

    public static int saturatedCast(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
