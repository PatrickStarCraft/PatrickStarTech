package com.gregtechceu.gtceu;

import net.minecraft.resources.Identifier;

/** Test-only identifier factory needed to initialize the real GT IO mode enum. */
public final class GTCEu {
    private GTCEu() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("gtceu", path);
    }
}
