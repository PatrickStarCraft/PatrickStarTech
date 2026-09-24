package com.gregtechceu.gtceu;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Narrow identifier and logging boundary for the real recipe index sources. */
public final class GTCEu {

    public static final Logger LOGGER = LoggerFactory.getLogger("gtceu-recipe-db-test");

    private GTCEu() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("gtceu", path);
    }

    public static boolean isDev() {
        return false;
    }
}
