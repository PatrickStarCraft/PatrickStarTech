package com.gregtechceu.gtceu.api.misc;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThermalFluidResourceHandlerPortTest {

    private static final int CAPACITY = 1000;

    @BeforeAll
    static void bindVanillaComponents() {
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
        for (Fluid fluid : new Fluid[] { Fluids.WATER, Fluids.LAVA }) {
            if (!fluid.builtInRegistryHolder().areComponentsBound()) {
                fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
            }
        }
    }

    private static ThermalFluidResourceHandler tank(ItemStack stack, int maxTemperature, boolean partial) {
        return new ThermalFluidResourceHandler(ItemAccess.forStack(stack), CAPACITY, maxTemperature,
                false, false, false, false, partial);
    }

    @Test
    void partialFillChecksTheTargetFluidTemperatureBoundary() {
        FluidStack waterStack = new FluidStack(Fluids.WATER, 1);
        int waterTemperature = waterStack.getFluidType().getTemperature(waterStack);
        FluidResource water = FluidResource.of(waterStack);

        ThermalFluidResourceHandler belowLimit = tank(new ItemStack(Items.PAPER), waterTemperature - 1, true);
        assertFalse(belowLimit.isValid(0, water));
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, belowLimit.insert(0, water, 500, transaction));
            transaction.commit();
        }

        ItemStack allowedStack = new ItemStack(Items.PAPER);
        ThermalFluidResourceHandler atLimit = tank(allowedStack, waterTemperature, true);
        assertTrue(atLimit.isValid(0, water));
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(500, atLimit.insert(0, water, 500, transaction));
            transaction.commit();
        }
        assertEquals(500, atLimit.getAmountAsLong(0));
    }

    @Test
    void fullOnlyFillAndDrainAreAtomicAndOuterAbortRollsBack() {
        ItemStack stack = new ItemStack(Items.PAPER);
        ThermalFluidResourceHandler handler = tank(stack, 0, false);
        FluidResource water = FluidResource.of(Fluids.WATER);

        try (Transaction outer = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(outer)) {
                assertEquals(1000, handler.insert(0, water, 1000, nested));
                nested.commit();
            }
            assertEquals(1000, handler.getAmountAsLong(0));
        }
        assertEquals(0, handler.getAmountAsLong(0));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, water, 999, transaction));
            assertEquals(1000, handler.insert(0, water, 1000, transaction));
            transaction.commit();
        }
        assertEquals(1000, handler.getAmountAsLong(0));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, handler.extract(0, water, 999, transaction));
            assertEquals(1000, handler.extract(0, water, 1000, transaction));
            transaction.commit();
        }
        assertEquals(0, handler.getAmountAsLong(0));
        assertNotNull(stack.get(com.gregtechceu.gtceu.common.data.GTDataComponents.FLUID_CONTENT.get()));
        assertTrue(stack.get(com.gregtechceu.gtceu.common.data.GTDataComponents.FLUID_CONTENT.get()).isEmpty());
    }
}
