package com.gregtechceu.gtceu.utils;

import java.util.Objects;

/** Safely reads and advances persisted enum ordinals. */
public final class EnumOrdinal {

    private EnumOrdinal() {}

    /**
     * Reads an enum ordinal, falling back to the first value for missing-era or malformed persisted data.
     */
    public static <E> E getOrDefault(E[] values, int ordinal) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) throw new IllegalArgumentException("Enum values must not be empty");
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
    }

    /**
     * Reads an enum ordinal using an explicit fallback, for modes whose established default is not ordinal zero.
     */
    public static <E> E getOrDefault(E[] values, int ordinal, E fallback) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(fallback, "fallback");
        if (values.length == 0) throw new IllegalArgumentException("Enum values must not be empty");
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    /** Returns the next ordinal, wrapping safely even when the saved current value is malformed. */
    public static int nextOrdinal(int ordinal, int valueCount) {
        if (valueCount <= 0) throw new IllegalArgumentException("valueCount must be positive");
        return (int) Math.floorMod((long) ordinal + 1L, (long) valueCount);
    }
}
