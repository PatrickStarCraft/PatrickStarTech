package com.gregtechceu.gtceu.api.capability.recipe;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Compile-only boundary used by MapIngredientTypeManager to read its content type and fallback. */
public abstract class RecipeCapability<T> {
    public final ContentSerializer<T> serializer;

    protected RecipeCapability(Class<T> contentClass) {
        this.serializer = new ContentSerializer<>(contentClass);
    }

    public List<AbstractMapIngredient> getDefaultMapIngredient(Object value) {
        return null;
    }

    public boolean isRecipeSearchFilter() {
        return true;
    }

    public List<Object> compressIngredients(Collection<Object> ingredients) {
        return new ArrayList<>(ingredients);
    }

    public record ContentSerializer<T>(Class<T> contentClass) {}
}
