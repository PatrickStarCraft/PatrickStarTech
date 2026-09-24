package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JetpackFuelSelectionPortTest {

    private static final FluidIngredient WATER_FUEL = FluidIngredient.of(Fluids.WATER, 100);
    private static final FluidIngredient LAVA_FUEL = FluidIngredient.of(Fluids.LAVA, 250);

    @BeforeAll
    static void bindFluidComponents() {
        for (Fluid fluid : new Fluid[] { Fluids.WATER, Fluids.LAVA }) {
            if (!fluid.builtInRegistryHolder().areComponentsBound()) {
                fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
            }
        }
    }

    @Test
    void reusesPreviousFuelOnlyWhenFluidAndMinimumAmountStillMatch() {
        assertSame(WATER_FUEL, JetpackFuelSelection.select(
                new FluidStack(Fluids.WATER, 100), WATER_FUEL, List.of(LAVA_FUEL, WATER_FUEL)));
        assertSame(FluidIngredient.EMPTY, JetpackFuelSelection.select(
                new FluidStack(Fluids.WATER, 99), WATER_FUEL, List.of(WATER_FUEL)));
    }

    @Test
    void selectsReplacementFuelAndClearsStaleMatchForUnacceptedOrUnderfilledFluid() {
        assertSame(LAVA_FUEL, JetpackFuelSelection.select(
                new FluidStack(Fluids.LAVA, 250), WATER_FUEL, List.of(WATER_FUEL, LAVA_FUEL)));
        assertSame(FluidIngredient.EMPTY, JetpackFuelSelection.select(
                new FluidStack(Fluids.LAVA, 249), WATER_FUEL, List.of(WATER_FUEL, LAVA_FUEL)));
        assertSame(FluidIngredient.EMPTY, JetpackFuelSelection.select(
                new FluidStack(Fluids.LAVA, 500), WATER_FUEL, List.of(WATER_FUEL)));
    }

    @Test
    void emptyFluidClearsCachedFuel() {
        assertSame(FluidIngredient.EMPTY,
                JetpackFuelSelection.select(FluidStack.EMPTY, WATER_FUEL, List.of(WATER_FUEL)));
    }

    @Test
    void tankFilterAcceptsPartialConfiguredFuelButRejectsOtherAndEmptyFluids() {
        assertTrue(JetpackFuelSelection.isFuelFluid(
                new FluidStack(Fluids.WATER, 1), List.of(WATER_FUEL)));
        assertFalse(JetpackFuelSelection.isFuelFluid(
                new FluidStack(Fluids.LAVA, 500), List.of(WATER_FUEL)));
        assertFalse(JetpackFuelSelection.isFuelFluid(FluidStack.EMPTY, List.of(WATER_FUEL)));
    }
}
