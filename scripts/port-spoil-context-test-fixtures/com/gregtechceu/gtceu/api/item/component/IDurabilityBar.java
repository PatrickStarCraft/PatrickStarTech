package com.gregtechceu.gtceu.api.item.component;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.jetbrains.annotations.Nullable;

/** Compile-only display contract; client rendering is not exercised by these tests. */
public interface IDurabilityBar {

    int getBarColor(ItemStack stack);

    boolean doDamagedStateColors(ItemStack itemStack);

    @Nullable IntIntPair getDurabilityColorsForDisplay(ItemStack itemStack);

    float getDurabilityForDisplay(ItemStack stack);
}
