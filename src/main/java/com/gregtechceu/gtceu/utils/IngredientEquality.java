package com.gregtechceu.gtceu.utils;

import net.minecraft.world.item.crafting.Ingredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;

import it.unimi.dsi.fastutil.Hash;

import java.util.Objects;

/**
 * Equality helpers for ingredients used as keys in recipe lookup maps.
 *
 * <p>Modern Minecraft ingredients own their value/custom-ingredient representation and
 * implement matching {@link Ingredient#equals(Object)} and {@link Ingredient#hashCode()}
 * semantics. Inspecting the old Forge {@code Ingredient.Value} internals here bypassed
 * custom ingredients and depended on APIs that no longer exist.</p>
 */
public final class IngredientEquality {

    private IngredientEquality() {}

    public static boolean ingredientEquals(Ingredient first, Ingredient second) {
        return Objects.equals(unwrapCounts(first), unwrapCounts(second));
    }

    private static Ingredient unwrapCounts(Ingredient ingredient) {
        while (ingredient != null) {
            var custom = ingredient.getCustomIngredient();
            if (custom instanceof SizedIngredient sized) {
                ingredient = sized.getInner();
            } else if (custom instanceof IntProviderIngredient provider) {
                ingredient = provider.getInner();
            } else {
                return ingredient;
            }
        }
        return null;
    }

    public static final class IngredientHashStrategy implements Hash.Strategy<Ingredient> {

        public static final IngredientHashStrategy INSTANCE = new IngredientHashStrategy();

        private IngredientHashStrategy() {}

        @Override
        public int hashCode(Ingredient ingredient) {
            Ingredient unwrapped = unwrapCounts(ingredient);
            return unwrapped == null ? 0 : unwrapped.hashCode();
        }

        @Override
        public boolean equals(Ingredient first, Ingredient second) {
            return ingredientEquals(first, second);
        }
    }
}
