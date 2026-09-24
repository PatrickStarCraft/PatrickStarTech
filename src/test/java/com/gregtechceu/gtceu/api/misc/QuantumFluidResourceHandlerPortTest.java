package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.api.item.data.ItemStackData;
import com.gregtechceu.gtceu.utils.data.StackPersistence;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuantumFluidResourceHandlerPortTest {

    @BeforeAll
    static void bindItemComponents() {
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
        if (!Fluids.WATER.builtInRegistryHolder().areComponentsBound()) {
            Fluids.WATER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }

    private static QuantumFluidResourceHandler handler(ItemStack stack, long capacity) {
        return new QuantumFluidResourceHandler(ItemAccess.forStack(stack), capacity);
    }

    @Test
    void closingRootRollsBackNestedFluidWrites() {
        var stack = new ItemStack(Items.PAPER);
        var handler = handler(stack, 1000);
        var water = FluidResource.of(Fluids.WATER);

        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                assertEquals(600, handler.insert(0, water, 600, nested));
                nested.commit();
            }
            assertEquals(600, handler.getAmountAsLong(0));
            // Leaving the root uncommitted models simulation and must undo its committed child.
        }

        assertEquals(0, handler.getAmountAsLong(0));
        assertTrue(handler.getResource(0).isEmpty());
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void uncommittedRootRollsBackBothInsertionAndExtraction() {
        var stack = new ItemStack(Items.PAPER);
        var handler = handler(stack, 1000);
        var water = FluidResource.of(Fluids.WATER);

        try (Transaction simulation = Transaction.openRoot()) {
            assertEquals(400, handler.insert(0, water, 400, simulation));
            assertEquals(400, handler.getAmountAsLong(0));
        }
        assertEquals(0, handler.getAmountAsLong(0));

        try (Transaction committed = Transaction.openRoot()) {
            assertEquals(400, handler.insert(0, water, 400, committed));
            committed.commit();
        }
        try (Transaction simulation = Transaction.openRoot()) {
            assertEquals(150, handler.extract(0, water, 150, simulation));
            assertEquals(250, handler.getAmountAsLong(0));
        }

        assertEquals(400, handler.getAmountAsLong(0));
        assertEquals(400, ItemStackData.read(stack).getLongOr("storedAmount", -1));
    }

    @Test
    void stackedTankItemCannotDuplicateFluidCapacity() {
        var stack = new ItemStack(Items.PAPER, 2);
        var handler = handler(stack, 1000);

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, FluidResource.of(Fluids.WATER), 500, transaction));
            transaction.commit();
        }

        assertEquals(2, stack.getCount());
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void committedTransfersPreserveFluidComponentsAndUnrelatedItemData() {
        var stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.MAX_STACK_SIZE, 16);
        CompoundTag initialData = new CompoundTag();
        initialData.putString("OtherMod", "keep");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(initialData));

        CompoundTag fluidData = new CompoundTag();
        fluidData.putString("Grade", "treated");
        FluidStack taggedWater = new FluidStack(Fluids.WATER, 1);
        taggedWater.set(DataComponents.CUSTOM_DATA, CustomData.of(fluidData));
        FluidResource treatedWater = FluidResource.of(taggedWater);
        var handler = handler(stack, 100);

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(100, handler.insert(0, treatedWater, 100, transaction));
            transaction.commit();
        }

        assertEquals(16, stack.get(DataComponents.MAX_STACK_SIZE));
        assertEquals("keep", ItemStackData.read(stack).getStringOr("OtherMod", ""));
        assertEquals("treated", handler.getResource(0).toStack(1)
                .get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("Grade", ""));
        assertEquals(0, handler.extract(0, FluidResource.of(Fluids.WATER), 100, null));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(40, handler.extract(0, treatedWater, 40, transaction));
            transaction.commit();
        }
        assertEquals(60, handler.getAmountAsLong(0));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(60, handler.extract(0, treatedWater, 60, transaction));
            transaction.commit();
        }
        assertTrue(handler.getResource(0).isEmpty());
        assertEquals("keep", ItemStackData.read(stack).getStringOr("OtherMod", ""));
        assertFalse(ItemStackData.read(stack).contains("stored"));
    }

    @Test
    void longCapacitySurvivesIntSizedTransfersAndExtraction() {
        var stack = new ItemStack(Items.PAPER);
        long capacity = (long) Integer.MAX_VALUE + 5;
        var handler = handler(stack, capacity);
        var water = FluidResource.of(Fluids.WATER);

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(Integer.MAX_VALUE, handler.insert(0, water, Integer.MAX_VALUE, transaction));
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(5, handler.insert(0, water, 50, transaction));
            transaction.commit();
        }
        assertEquals(capacity, handler.getAmountAsLong(0));
        assertEquals(capacity, ItemStackData.read(stack).getLongOr("storedAmount", -1));

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(Integer.MAX_VALUE, handler.extract(0, water, Integer.MAX_VALUE, transaction));
            transaction.commit();
        }
        assertEquals(5, handler.getAmountAsLong(0));
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(5, handler.extract(0, water, 50, transaction));
            transaction.commit();
        }
        assertEquals(0, handler.getAmountAsLong(0));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void fillingLongMaxCapacityDoesNotOverflowTheRemainingAmount() {
        var stack = new ItemStack(Items.PAPER);
        CompoundTag data = new CompoundTag();
        data.put("stored", StackPersistence.saveFluid(new FluidStack(Fluids.WATER, 1)));
        data.putLong("storedAmount", Long.MAX_VALUE - 4);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var handler = handler(stack, Long.MAX_VALUE);
        var water = FluidResource.of(Fluids.WATER);

        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(4, handler.insert(0, water, 10, transaction));
            transaction.commit();
        }

        assertEquals(Long.MAX_VALUE, handler.getAmountAsLong(0));
        assertEquals(Long.MAX_VALUE, ItemStackData.read(stack).getLongOr("storedAmount", -1));
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(0, handler.insert(0, water, 1, transaction));
            transaction.commit();
        }
        assertEquals(Long.MAX_VALUE, handler.getAmountAsLong(0));
    }

    @Test
    void malformedSavedFluidIsIgnoredWithoutMutatingItemData() {
        var stack = new ItemStack(Items.PAPER);
        CompoundTag malformedFluid = new CompoundTag();
        malformedFluid.putString("id", "not a fluid id");
        malformedFluid.putInt("amount", 100);
        CompoundTag data = new CompoundTag();
        data.put("stored", malformedFluid);
        data.putLong("storedAmount", 100);
        data.putString("OtherMod", "keep");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        var handler = handler(stack, 1000);
        assertEquals(0, handler.getAmountAsLong(0));
        assertTrue(handler.getResource(0).isEmpty());
        assertEquals(data, ItemStackData.read(stack));
    }
}
