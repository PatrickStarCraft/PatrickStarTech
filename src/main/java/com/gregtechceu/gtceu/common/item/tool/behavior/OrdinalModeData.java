package com.gregtechceu.gtceu.common.item.tool.behavior;

import java.util.Objects;

/** Bounds-checks persisted mode ordinals before indexing mode arrays. */
public final class OrdinalModeData {

    private OrdinalModeData() {}

    public static <T> T getMode(T[] modes, int ordinal, T fallback) {
        Objects.requireNonNull(modes, "modes");
        Objects.requireNonNull(fallback, "fallback");
        if (modes.length == 0) throw new IllegalArgumentException("Mode list must not be empty");
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : fallback;
    }

    public static int nextOrdinal(int ordinal, int modeCount) {
        if (modeCount <= 0) throw new IllegalArgumentException("modeCount must be positive");
        return (int) Math.floorMod((long) ordinal + 1L, (long) modeCount);
    }
}
