package com.gregtechceu.gtceu;

import net.minecraft.resources.Identifier;

/** Identifier utility fixture for isolated compilation of the thermal fluid contract. */
public final class GTCEu {

    public static final String MOD_ID = "gtceu";

    private GTCEu() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
