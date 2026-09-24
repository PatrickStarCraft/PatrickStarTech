package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;

public final class RecipeHelper {

    private RecipeHelper() {}

    public static MatchResult matchRecipe(IRecipeCapabilityHolder holder, GTRecipe recipe) {
        return new MatchResult();
    }

    public record MatchResult(boolean isSuccess) {
        public MatchResult() {
            this(true);
        }
    }
}
