package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.data.ItemStackData;

import net.minecraft.world.item.ItemStack;

/** Reads and commits the active flag stored on an item magnet. */
final class MagnetStateData {

    private static final String ACTIVE_KEY = "IsActive";

    private MagnetStateData() {}

    static boolean isActive(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ItemStackData.read(stack).getBooleanOr(ACTIVE_KEY, false);
    }

    static boolean toggleActive(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean active = !isActive(stack);
        ItemStackData.update(stack, data -> data.putBoolean(ACTIVE_KEY, active));
        return active;
    }
}
