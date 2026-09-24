package com.gregtechceu.gtceu.api.recipe;

/** Compile-only EU/t contract for the unexecuted getModifier path. */
public final class RecipeHelper {
    private RecipeHelper() {}

    public static TotalEU getRealEUt(GTRecipe recipe) {
        return new TotalEU();
    }

    public static final class TotalEU {
        public long getTotalEU() {
            return 0;
        }
    }
}
