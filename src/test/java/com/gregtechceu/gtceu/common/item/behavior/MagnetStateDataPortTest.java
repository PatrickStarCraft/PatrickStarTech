package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.data.ItemStackData;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagnetStateDataPortTest {

    @BeforeAll
    static void bindTestItemComponents() {
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }

    @Test
    void aNewMagnetIsInactiveWithoutCreatingCustomData() {
        ItemStack stack = new ItemStack(Items.PAPER);

        assertFalse(MagnetStateData.isActive(stack));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void togglingPersistsTheActiveFlagAndPreservesUnrelatedData() {
        ItemStack stack = new ItemStack(Items.PAPER);
        ItemStackData.update(stack, data -> data.putString("other_mod", "keep"));

        assertTrue(MagnetStateData.toggleActive(stack));
        assertTrue(MagnetStateData.isActive(stack));
        assertEquals("keep", ItemStackData.read(stack).getStringOr("other_mod", ""));

        assertFalse(MagnetStateData.toggleActive(stack));
        assertFalse(MagnetStateData.isActive(stack));
        assertEquals("keep", ItemStackData.read(stack).getStringOr("other_mod", ""));
    }

    @Test
    void aCopiedMagnetKeepsAnIndependentSavedActiveFlag() {
        ItemStack original = new ItemStack(Items.PAPER);
        MagnetStateData.toggleActive(original);
        ItemStack copy = original.copy();

        assertTrue(MagnetStateData.isActive(copy));
        assertFalse(MagnetStateData.toggleActive(original));
        assertFalse(MagnetStateData.isActive(original));
        assertTrue(MagnetStateData.isActive(copy));
    }

    @Test
    void noncanonicalEmptyStacksIgnoreAndRetainTheirStoredFlags() {
        ItemStack emptyStack = new ItemStack(Items.PAPER, 0);
        assertNotSame(ItemStack.EMPTY, emptyStack);
        assertTrue(emptyStack.isEmpty());
        ItemStackData.update(emptyStack, data -> data.putBoolean("IsActive", true));
        CompoundTag before = ItemStackData.read(emptyStack);

        assertFalse(MagnetStateData.isActive(emptyStack));
        assertFalse(MagnetStateData.toggleActive(emptyStack));
        assertEquals(before, ItemStackData.read(emptyStack));
    }
}
