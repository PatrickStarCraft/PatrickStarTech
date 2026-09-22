package com.gregtechceu.gtceu.api.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import com.mojang.serialization.MapCodec;

import java.util.stream.Stream;

/** Explicit empty GT content; vanilla represents absent crafting slots with Optional instead. */
public record EmptyIngredient() implements ICustomIngredient {
    public static final EmptyIngredient INSTANCE = new EmptyIngredient();
    public static final MapCodec<EmptyIngredient> CODEC = MapCodec.unit(INSTANCE);
    public static final IngredientType<EmptyIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);
    public static final Ingredient VANILLA = INSTANCE.toVanilla();

    @Override
    public boolean test(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.empty();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return INGREDIENT_TYPE;
    }
}
