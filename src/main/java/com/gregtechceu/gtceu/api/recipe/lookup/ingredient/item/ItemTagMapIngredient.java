package com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemTagMapIngredient extends AbstractMapIngredient {

    protected TagKey<Item> tag;

    public ItemTagMapIngredient(TagKey<Item> tag) {
        this.tag = tag;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(Ingredient ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>();
        if (ingredient.getCustomIngredient() != null) return ingredients;
        // A tag key can be used before its holder set is bound. This map key only needs
        // the symbolic tag id; checking isEmpty() would try to expand the tag contents.
        ingredient.getValues().unwrapKey().ifPresent(tag -> ingredients.add(new ItemTagMapIngredient(tag)));
        return ingredients;
    }

    @NotNull
    public static List<AbstractMapIngredient> from(ItemStack stack) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>();
        stack.typeHolder().tags().forEach(tag -> ingredients.add(new ItemTagMapIngredient(tag)));
        return ingredients;
    }

    @Override
    protected int hash() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return tag == ((ItemTagMapIngredient) obj).tag;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ItemTagMapIngredient{" + "tag=" + tag.location() + "}";
    }
}
