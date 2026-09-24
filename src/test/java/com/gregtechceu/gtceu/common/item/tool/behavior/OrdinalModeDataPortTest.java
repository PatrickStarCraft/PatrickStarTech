package com.gregtechceu.gtceu.common.item.tool.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdinalModeDataPortTest {

    private static final String[] MODES = { "item", "fluid", "both" };

    @Test
    void validAndMissingOrdinalsKeepTheirExistingDefault() {
        assertEquals("item", OrdinalModeData.getMode(MODES, 0, "item"));
        assertEquals("fluid", OrdinalModeData.getMode(MODES, 1, "item"));
        assertEquals("both", OrdinalModeData.getMode(MODES, 2, "item"));
    }

    @Test
    void malformedOrdinalsFallBackInsteadOfIndexingOutsideTheModeList() {
        assertEquals("item", OrdinalModeData.getMode(MODES, -1, "item"));
        assertEquals("item", OrdinalModeData.getMode(MODES, Byte.MIN_VALUE, "item"));
        assertEquals("item", OrdinalModeData.getMode(MODES, 3, "item"));
        assertEquals("item", OrdinalModeData.getMode(MODES, Integer.MAX_VALUE, "item"));
    }

    @Test
    void cyclingWrapsForNormalAndMalformedOrdinals() {
        assertEquals(1, OrdinalModeData.nextOrdinal(0, MODES.length));
        assertEquals(2, OrdinalModeData.nextOrdinal(1, MODES.length));
        assertEquals(0, OrdinalModeData.nextOrdinal(2, MODES.length));
        assertEquals(0, OrdinalModeData.nextOrdinal(-1, MODES.length));
        assertEquals(2, OrdinalModeData.nextOrdinal(Byte.MAX_VALUE, MODES.length));
    }

    @Test
    void emptyModesAndNonPositiveCycleCountsFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> OrdinalModeData.getMode(new String[0], 0, "item"));
        assertThrows(IllegalArgumentException.class, () -> OrdinalModeData.nextOrdinal(0, 0));
    }
}
