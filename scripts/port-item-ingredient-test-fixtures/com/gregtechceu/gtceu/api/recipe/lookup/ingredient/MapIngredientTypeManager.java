package com.gregtechceu.gtceu.api.recipe.lookup.ingredient;

import java.util.List;

/** Compile-only fixture: the selected tests exercise map-key equality, not factory dispatch. */
public final class MapIngredientTypeManager {
    private MapIngredientTypeManager() {}

    public static List<AbstractMapIngredient> getFrom(Object ingredient, Object capability) {
        return List.of();
    }
}
