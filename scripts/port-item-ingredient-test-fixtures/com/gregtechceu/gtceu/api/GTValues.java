package com.gregtechceu.gtceu.api;

import net.minecraft.util.RandomSource;

/** The real ingredient interface needs only GT's shared random source in this selected-source test. */
public final class GTValues {

    public static final RandomSource RNG = RandomSource.createThreadSafe();

    private GTValues() {}
}
