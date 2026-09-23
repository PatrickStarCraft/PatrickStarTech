package com.gregtechceu.gtceu.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Supplies block-specific tooltip lines from the block's item. */
public interface IBlockItemTooltip {

    void appendBlockItemTooltip(ItemStack stack, Consumer<Component> tooltip);
}
