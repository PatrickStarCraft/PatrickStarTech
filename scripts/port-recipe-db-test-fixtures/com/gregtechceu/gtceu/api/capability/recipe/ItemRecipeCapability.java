package com.gregtechceu.gtceu.api.capability.recipe;

import net.minecraft.world.item.crafting.Ingredient;

public final class ItemRecipeCapability extends RecipeCapability<Ingredient> {

    public static final ItemRecipeCapability CAP = new ItemRecipeCapability();

    private ItemRecipeCapability() {
        super(Ingredient.class);
    }
}
