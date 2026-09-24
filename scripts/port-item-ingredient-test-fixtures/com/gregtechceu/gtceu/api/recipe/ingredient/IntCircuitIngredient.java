package com.gregtechceu.gtceu.api.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/** Unused sibling type needed to compile the real IngredientStacks dispatch table. */
final class IntCircuitIngredient implements ICustomIngredient {

    public ItemStack[] getItems() { return new ItemStack[0]; }
    @Override public boolean test(ItemStack stack) { return false; }
    @Override public Stream<Holder<Item>> items() { return Stream.empty(); }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return null; }
}
