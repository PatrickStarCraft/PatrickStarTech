package com.gregtechceu.gtceu.api.capability.recipe;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

public final class FluidRecipeCapability extends RecipeCapability<FluidIngredient> {

    public static final FluidRecipeCapability CAP = new FluidRecipeCapability();

    private FluidRecipeCapability() {
        super(FluidIngredient.class);
    }

    public FluidIngredient of(Object content) {
        return (FluidIngredient) content;
    }
}
