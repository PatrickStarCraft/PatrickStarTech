package com.gregtechceu.gtceu.api.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverclockingLogicPortTest {

    @Test
    void standardLogicStopsAtVoltageAndOneTickBoundaries() {
        var twoOverclocks = new OverclockingLogic.OCParams(8, 16, 2, 16);
        var result = OverclockingLogic.standardOC(twoOverclocks, 512,
                OverclockingLogic.STD_DURATION_FACTOR, OverclockingLogic.STD_VOLTAGE_FACTOR);
        assertEquals(2, result.ocLevel());
        assertEquals(16.0, result.eutMultiplier());
        assertEquals(0.25, result.durationMultiplier());
        assertEquals(1, result.parallels());

        var blockedByVoltage = OverclockingLogic.standardOC(twoOverclocks, 31,
                OverclockingLogic.STD_DURATION_FACTOR, OverclockingLogic.STD_VOLTAGE_FACTOR);
        assertEquals(0, blockedByVoltage.ocLevel());

        var oneTick = new OverclockingLogic.OCParams(8, 1, 3, 16);
        var blockedByMinimumDuration = OverclockingLogic.standardOC(oneTick, 512,
                OverclockingLogic.STD_DURATION_FACTOR, OverclockingLogic.STD_VOLTAGE_FACTOR);
        assertEquals(0, blockedByMinimumDuration.ocLevel());
        assertEquals(1.0, blockedByMinimumDuration.durationMultiplier());
    }

    @Test
    void subtickNonParallelLogicTradesVoltageForAOneTickRecipe() {
        var oneTick = new OverclockingLogic.OCParams(16, 1, 1, 1);
        var result = OverclockingLogic.subTickNonParallelOC(oneTick, 64,
                OverclockingLogic.STD_DURATION_FACTOR, OverclockingLogic.STD_VOLTAGE_FACTOR);

        assertEquals(1, result.ocLevel());
        assertEquals(2.0, result.eutMultiplier());
        assertEquals(1.0, result.durationMultiplier());
        assertEquals(1, result.parallels());
    }

    @Test
    void subtickParallelLogicUsesAndRespectsTheConfiguredParallelLimit() {
        var oneTick = new OverclockingLogic.OCParams(16, 1, 3, 4);
        var result = OverclockingLogic.subTickParallelOC(oneTick, 1_000_000,
                OverclockingLogic.PERFECT_DURATION_FACTOR, OverclockingLogic.STD_VOLTAGE_FACTOR);

        assertEquals(1, result.ocLevel(), "a second perfect subtick would exceed the max parallel count");
        assertEquals(4.0, result.eutMultiplier());
        assertEquals(1.0, result.durationMultiplier());
        assertEquals(4, result.parallels());
    }

    @Test
    void heatingCoilLogicAppliesOnlyWholeDiscountSteps() {
        assertEquals(1.0, OverclockingLogic.getCoilEUtDiscount(899, 5_000));
        assertEquals(1.0, OverclockingLogic.getCoilEUtDiscount(1_800, 1_799));
        assertEquals(0.95, OverclockingLogic.getCoilEUtDiscount(900, 1_800), 1.0e-12);
        assertEquals(0.95 * 0.95, OverclockingLogic.getCoilEUtDiscount(900, 2_700), 1.0e-12);

        var params = new OverclockingLogic.OCParams(16, 16, 3, 64);
        var result = OverclockingLogic.heatingCoilOC(params, 1_024, 900, 2_700);
        assertEquals(3, result.ocLevel());
        assertEquals(64.0, result.eutMultiplier());
        assertEquals(0.0625, result.durationMultiplier());
        assertEquals(1, result.parallels());
    }
}
