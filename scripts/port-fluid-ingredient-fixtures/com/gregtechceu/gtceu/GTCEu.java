package com.gregtechceu.gtceu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated test shim for logging and the optional-mod flag; not part of production sources. */
public final class GTCEu {
    public static final Logger LOGGER = LoggerFactory.getLogger("gtceu-fluid-ingredient-test");

    public static final class Mods {
        public static boolean isKubeJSLoaded() {
            return false;
        }
    }

    private GTCEu() {}
}
