package com.gregtechceu.gtceu.api.transfer.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Compile-only inventory adapter for SpoilContext's untested player-inventory branch. */
public final class CustomItemStackHandler extends ItemStackHandler {

    public CustomItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }
}
