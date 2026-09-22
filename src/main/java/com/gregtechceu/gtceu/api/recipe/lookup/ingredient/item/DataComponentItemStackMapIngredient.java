package com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item;

import com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/** Lookup key for component-sensitive NeoForge ingredients and inventory stacks. */
public final class DataComponentItemStackMapIngredient extends AbstractMapIngredient {

    private final ItemStack stack;
    private final DataComponentIngredient ingredient;

    public DataComponentItemStackMapIngredient(ItemStack stack, DataComponentIngredient ingredient) {
        this.stack = stack;
        this.ingredient = ingredient;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(DataComponentIngredient ingredient) {
        Ingredient wrapped = ingredient.toVanilla();
        ObjectArrayList<AbstractMapIngredient> result = new ObjectArrayList<>();
        for (ItemStack stack : IngredientStacks.getItems(wrapped)) {
            result.add(new DataComponentItemStackMapIngredient(stack, ingredient));
        }
        return result;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(ItemStack stack) {
        return Collections.singletonList(new DataComponentItemStackMapIngredient(stack, null));
    }

    @Override
    protected int hash() {
        // Stack size and components are intentionally excluded from the lookup bucket.
        return ItemStackHashStrategy.comparingItem().hashCode(stack) * 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            DataComponentItemStackMapIngredient other = (DataComponentItemStackMapIngredient) obj;
            if (stack.getItem() != other.stack.getItem()) return false;
            if (ingredient != null) {
                if (other.ingredient != null) {
                    return ingredient.itemSet().equals(other.ingredient.itemSet())
                            && ingredient.components().equals(other.ingredient.components())
                            && ingredient.componentsExhaustive() == other.ingredient.componentsExhaustive();
                }
                return ingredient.test(other.stack);
            }
            return other.ingredient == null
                    ? ItemStack.isSameItemSameComponents(stack, other.stack)
                    : other.ingredient.test(stack);
        }
        return false;
    }

    @Override
    public boolean isSpecialIngredient() {
        return true;
    }
}
