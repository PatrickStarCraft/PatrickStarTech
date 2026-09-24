package com.gregtechceu.gtceu.api;

import net.minecraft.util.RandomSource;

/** Isolated test fixture for IRangedIngredient's default RNG reference. */
public final class GTValues {
    public static final RandomSource RNG = RandomSource.createThreadSafe();

    private GTValues() {}
}
