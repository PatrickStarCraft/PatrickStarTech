package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public interface ItemStackAccessor {

    @Accessor("item")
    @Nullable Holder<Item> gtceu$getItemHolder();

    @Accessor("components")
    PatchedDataComponentMap gtceu$getComponents();
}
