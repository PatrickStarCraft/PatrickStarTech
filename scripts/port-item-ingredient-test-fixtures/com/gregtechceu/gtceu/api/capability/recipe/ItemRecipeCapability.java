package com.gregtechceu.gtceu.api.capability.recipe;

import net.minecraft.world.item.crafting.Ingredient;

/** Compile-only capability boundary for the selected ingredient-dispatch tests. */
public final class ItemRecipeCapability {
    public static final RecipeCapability<Ingredient> CAP = new RecipeCapability<>(Ingredient.class) {};

    private ItemRecipeCapability() {}
}
