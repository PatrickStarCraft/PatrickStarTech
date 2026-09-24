package com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks;
import com.gregtechceu.gtceu.utils.IngredientEquality;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;

public class ItemStackMapIngredient extends AbstractMapIngredient {

    protected ItemStack stack;
    protected Ingredient ingredient = null;

    public ItemStackMapIngredient(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStackMapIngredient(ItemStack stack, Ingredient ingredient) {
        this.stack = stack;
        this.ingredient = ingredient;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(Ingredient ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>();
        // Tags have their own lookup keys. Expanding them here also changes intersection
        // keys into a conjunction of every tagged item instead of a conjunction of tags.
        if (!ingredient.isCustom() && ingredient.getValues().unwrapKey().isPresent()) return ingredients;
        if (ingredient.getCustomIngredient() instanceof net.neoforged.neoforge.common.crafting.CompoundIngredient compound) {
            for (Ingredient child : compound.children()) {
                ingredients.addAll(com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager
                        .getFrom(child, com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability.CAP));
            }
            return ingredients;
        }
        for (ItemStack stack : IngredientStacks.getItems(ingredient)) {
            ingredients.add(new ItemStackMapIngredient(stack, ingredient));
        }
        return ingredients;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(CompoundIngredient ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>();
        for (Ingredient child : ingredient.children()) {
            ingredients.addAll(com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager
                    .getFrom(child, com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability.CAP));
        }
        return ingredients;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(ItemStack stack) {
        return Collections.singletonList(new ItemStackMapIngredient(stack));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntersectionMapIngredient intersection) {
            return intersection.matchesItemStack(this);
        }
        if (super.equals(o)) {
            ItemStackMapIngredient other = (ItemStackMapIngredient) o;
            if (!ItemStack.isSameItem(this.stack, other.stack)) {
                return false;
            }
            if (this.ingredient != null) {
                if (other.ingredient != null) {
                    return IngredientEquality.ingredientEquals(this.ingredient, other.ingredient);
                } else {
                    return this.ingredient.test(other.stack);
                }
            } else if (other.ingredient != null) {
                return other.ingredient.test(this.stack);
            } else {
                return ItemStack.isSameItemSameComponents(this.stack, other.stack);
            }
        }
        return false;
    }

    @Override
    protected int hash() {
        return stack.getItem().hashCode() * 31;
    }

    @Override
    public String toString() {
        return "ItemStackMapIngredient{" + "item=" + stack + "}";
    }
}
