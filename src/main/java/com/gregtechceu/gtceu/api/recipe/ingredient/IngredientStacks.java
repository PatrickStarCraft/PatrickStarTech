package com.gregtechceu.gtceu.api.recipe.ingredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;

import java.util.Arrays;

/** Utilities for obtaining representative stacks from 26.2 ingredients. */
public final class IngredientStacks {

    private IngredientStacks() {}

    /** Unwrap item delegates while leaving fluid ingredients and other content unchanged. */
    public static Object unwrap(Object content) {
        if (content instanceof Ingredient ingredient && ingredient.getCustomIngredient() != null) {
            return ingredient.getCustomIngredient();
        }
        return content;
    }

    /**
     * Returns representative stacks for an ingredient, retaining data components when it is a
     * NeoForge component ingredient.
     */
    public static ItemStack[] getItems(Ingredient ingredient) {
        if (ingredient.isEmpty()) return new ItemStack[0];

        var custom = ingredient.getCustomIngredient();
        if (custom instanceof SizedIngredient sized) return sized.getItems();
        if (custom instanceof IntProviderIngredient ranged) return ranged.getItems();
        if (custom instanceof IntCircuitIngredient circuit) return circuit.getItems();
        if (custom instanceof NBTPredicateIngredient predicate) return predicate.getItems();
        if (custom instanceof FluidContainerIngredient container) return container.getItems();
        if (custom instanceof CompoundIngredient compound) {
            return compound.children().stream().flatMap(child -> Arrays.stream(getItems(child)))
                    .toArray(ItemStack[]::new);
        }
        if (custom instanceof IntersectionIngredient intersection) {
            // Any child may supply the components needed by the intersection, not just the first.
            return intersection.children().stream().flatMap(child -> Arrays.stream(getItems(child)))
                    .filter(intersection::test).toArray(ItemStack[]::new);
        }

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
