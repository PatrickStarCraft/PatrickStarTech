package com.gregtechceu.gtceu.api.item;

import net.minecraft.world.item.ItemStack;

/** Compile-only stack replacement hook; the selected tests do not trigger spoil transformation. */
public interface ISpoilableItemStackExtension {

    void gtceu$setStack(ItemStack newStack);
}
