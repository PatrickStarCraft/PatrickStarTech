package com.gregtechceu.gtceu;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Narrow identifier and logging fixture for the selected ingredient sources. */
public final class GTCEu {

    public static final Logger LOGGER = LoggerFactory.getLogger("gtceu-item-ingredient-test");

    private GTCEu() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("gtceu", path);
    }
}
