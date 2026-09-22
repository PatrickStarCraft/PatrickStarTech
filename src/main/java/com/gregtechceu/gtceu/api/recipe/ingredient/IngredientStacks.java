package com.gregtechceu.gtceu.api.recipe.ingredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/** Utilities for obtaining representative stacks from 26.2 ingredients. */
public final class IngredientStacks {

    private IngredientStacks() {}

    /**
     * Returns representative stacks for an ingredient, retaining data components when it is a
     * NeoForge component ingredient.
     */
    public static ItemStack[] getItems(Ingredient ingredient) {
        if (ingredient.isEmpty()) return new ItemStack[0];

        if (ingredient.getCustomIngredient() instanceof DataComponentIngredient componentIngredient) {
            return componentIngredient.itemSet().stream().map(holder -> {
                ItemStack stack = new ItemStack(holder);
                stack.applyComponents(componentIngredient.components());
                return stack;
            }).toArray(ItemStack[]::new);
        }

        return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new);
    }
}
