package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.data.ItemStackData;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScannerModeDataPortTest {

    private enum Mode { ALL, BLOCK, MACHINE, ELECTRICAL }

    @BeforeAll
    static void bindTestItemComponents() {
        if (!Items.PAPER.builtInRegistryHolder().areComponentsBound()) {
            Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }

    @Test
    void absentModeDefaultsToFirstWithoutCreatingCustomData() {
        ItemStack stack = new ItemStack(Items.PAPER);

        assertSame(Mode.ALL, ScannerModeData.getMode(stack, Mode.values()));
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
        assertSame(Mode.ALL, ScannerModeData.getMode(ItemStack.EMPTY, Mode.values()));
    }

    @Test
    void advancingVisitsEveryModeAndWrapsToTheFirst() {
        ItemStack stack = new ItemStack(Items.PAPER);

        for (Mode expected : Mode.values()) {
            assertSame(expected, ScannerModeData.getMode(stack, Mode.values()));
            ScannerModeData.setNextMode(stack, Mode.values().length);
        }

        assertSame(Mode.ALL, ScannerModeData.getMode(stack, Mode.values()));
        assertEquals(0, ItemStackData.read(stack).getIntOr("Mode", -1));
    }

    @Test
    void malformedOrdinalsWrapAndAdvancingPreservesOtherDataAndCopies() {
        ItemStack stack = new ItemStack(Items.PAPER);
        ItemStackData.update(stack, data -> {
            data.putInt("Mode", -1);
            data.putString("other_mod", "preserve");
        });

        assertSame(Mode.ELECTRICAL, ScannerModeData.getMode(stack, Mode.values()));
        ItemStack copy = stack.copy();
        ScannerModeData.setNextMode(stack, Mode.values().length);

        assertSame(Mode.ALL, ScannerModeData.getMode(stack, Mode.values()));
        assertSame(Mode.ELECTRICAL, ScannerModeData.getMode(copy, Mode.values()));
        assertEquals("preserve", ItemStackData.read(stack).getStringOr("other_mod", ""));
        assertEquals("preserve", ItemStackData.read(copy).getStringOr("other_mod", ""));
    }

    @Test
    void sharedModeStorageSupportsProspectorModeValues() {
        ItemStack stack = new ItemStack(Items.PAPER);
        String[] modes = { "blocks", "fluids", "ores" };

        assertEquals("blocks", ScannerModeData.getMode(stack, modes));
        ScannerModeData.setNextMode(stack, modes.length);
        assertEquals("fluids", ScannerModeData.getMode(stack, modes));
        assertEquals(1, ItemStackData.read(stack).getIntOr("Mode", -1));
    }
}
