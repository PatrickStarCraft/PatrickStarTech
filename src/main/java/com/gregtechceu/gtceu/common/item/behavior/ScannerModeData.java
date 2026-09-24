package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.data.ItemStackData;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Shared stack-data boundary for persisted scanner mode ordinals. */
final class ScannerModeData {

    private static final String MODE_KEY = "Mode";

    private ScannerModeData() {}

    static <T> T getMode(ItemStack stack, T[] modes) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(modes, "modes");
        if (modes.length == 0) throw new IllegalArgumentException("Scanner needs at least one mode");
        if (stack.isEmpty()) return modes[0];
        int index = Math.floorMod(ItemStackData.read(stack).getIntOr(MODE_KEY, 0), modes.length);
        return modes[index];
    }

    static void setNextMode(ItemStack stack, int modeCount) {
        Objects.requireNonNull(stack, "stack");
        if (modeCount <= 0) throw new IllegalArgumentException("modeCount must be positive");
        ItemStackData.update(stack,
                tag -> tag.putInt(MODE_KEY, Math.floorMod(tag.getIntOr(MODE_KEY, 0) + 1, modeCount)));
    }
}
