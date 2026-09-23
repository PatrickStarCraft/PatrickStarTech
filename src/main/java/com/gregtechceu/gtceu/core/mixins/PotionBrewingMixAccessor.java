package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.Ingredient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.item.alchemy.PotionBrewing$Mix")
public interface PotionBrewingMixAccessor {

    @Accessor("from")
    Holder<?> gtceu$getFrom();

    @Accessor("ingredient")
    Ingredient gtceu$getIngredient();

    @Accessor("to")
    Holder<?> gtceu$getTo();
}
