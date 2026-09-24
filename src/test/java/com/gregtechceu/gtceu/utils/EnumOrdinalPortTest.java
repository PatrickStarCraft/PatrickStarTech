package com.gregtechceu.gtceu.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumOrdinalPortTest {

    private enum Mode {
        FIRST,
        SECOND,
        THIRD
    }

    @Test
    void validOrdinalsPreserveTheExistingEnumMapping() {
        assertEquals(Mode.FIRST, EnumOrdinal.getOrDefault(Mode.values(), 0));
        assertEquals(Mode.SECOND, EnumOrdinal.getOrDefault(Mode.values(), 1));
        assertEquals(Mode.THIRD, EnumOrdinal.getOrDefault(Mode.values(), 2));
    }

    @Test
    void malformedOrdinalsFallBackWithoutThrowing() {
        assertEquals(Mode.FIRST, EnumOrdinal.getOrDefault(Mode.values(), -1));
        assertEquals(Mode.FIRST, EnumOrdinal.getOrDefault(Mode.values(), Integer.MIN_VALUE));
        assertEquals(Mode.FIRST, EnumOrdinal.getOrDefault(Mode.values(), 3));
        assertEquals(Mode.FIRST, EnumOrdinal.getOrDefault(Mode.values(), Integer.MAX_VALUE));
        assertEquals(Mode.SECOND, EnumOrdinal.getOrDefault(Mode.values(), -1, Mode.SECOND));
    }

    @Test
    void nextOrdinalWrapsForValidAndMalformedValues() {
        assertEquals(1, EnumOrdinal.nextOrdinal(0, 3));
        assertEquals(0, EnumOrdinal.nextOrdinal(2, 3));
        assertEquals(0, EnumOrdinal.nextOrdinal(-1, 3));
        assertEquals(2, EnumOrdinal.nextOrdinal(Integer.MAX_VALUE, 3));
    }

    @Test
    void invalidArrayAndCycleSizesAreRejectedClearly() {
        assertThrows(IllegalArgumentException.class, () -> EnumOrdinal.getOrDefault(new Mode[0], 0));
        assertThrows(IllegalArgumentException.class, () -> EnumOrdinal.nextOrdinal(0, 0));
    }
}
