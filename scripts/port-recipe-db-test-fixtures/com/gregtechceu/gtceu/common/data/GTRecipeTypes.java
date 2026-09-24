package com.gregtechceu.gtceu.common.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;

public final class GTRecipeTypes {

    public static final RecipeType<?> COMBUSTION_GENERATOR_FUELS =
            RecipeType.simple(Identifier.fromNamespaceAndPath("gtceu_test", "combustion"));

    private GTRecipeTypes() {}
}
