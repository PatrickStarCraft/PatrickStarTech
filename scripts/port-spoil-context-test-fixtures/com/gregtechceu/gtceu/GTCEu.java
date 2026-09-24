package com.gregtechceu.gtceu;

import net.minecraft.resources.Identifier;

/** Identifier helper fixture for compiling the selected spoil-context source. */
public final class GTCEu {

    private GTCEu() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("gtceu", path);
    }

    public static boolean isClientThread() {
        return false;
    }
}
