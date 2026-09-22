package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.NBTPredicate;
import com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.NBTPredicates;
import com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.TrueNBTPredicate;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.stream.Stream;

public final class NBTPredicateIngredient implements ICustomIngredient {

    public static final Identifier TYPE = GTCEu.id("nbt_predicate");
    public static final NBTPredicate ALWAYS_TRUE = new TrueNBTPredicate();
    private static final Codec<NBTPredicate> PREDICATE_CODEC = net.minecraft.util.ExtraCodecs.JSON.xmap(
            json -> NBTPredicates.fromJson(net.minecraft.util.GsonHelper.convertToJsonObject(json, "predicate")),
            NBTPredicate::toJson);
    public static final MapCodec<NBTPredicateIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(ingredient -> ingredient.stack),
            PREDICATE_CODEC.fieldOf("predicate").forGetter(ingredient -> ingredient.predicate))
            .apply(instance, NBTPredicateIngredient::new));
    public static final IngredientType<NBTPredicateIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);

    private final NBTPredicate predicate;
    private final ItemStack stack;

    public NBTPredicateIngredient(ItemStack stack, NBTPredicate predicate) {
        this.stack = stack.copy();
        this.predicate = predicate;
    }

    protected NBTPredicateIngredient(ItemStack stack) {
        this(stack, ALWAYS_TRUE);
    }

    public static net.minecraft.world.item.crafting.Ingredient of(ItemStack stack, NBTPredicate predicate) {
        return new NBTPredicateIngredient(stack, predicate).toVanilla();
    }

    public static net.minecraft.world.item.crafting.Ingredient of(ItemStack stack) {
        return of(stack, ALWAYS_TRUE);
    }

    @Override
    public boolean test(ItemStack input) {
        return !input.isEmpty() && this.stack.getItem() == input.getItem() &&
                predicate.test(com.gregtechceu.gtceu.api.item.data.ItemStackData.read(input));
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(stack.typeHolder());
    }

    public ItemStack[] getItems() {
        return new ItemStack[] { this.stack.copy() };
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<NBTPredicateIngredient> getType() {
        return INGREDIENT_TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NBTPredicateIngredient other &&
                ItemStack.matches(this.stack, other.stack) &&
                this.predicate.toJson().equals(other.predicate.toJson());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStack.hashItemAndComponents(stack), predicate.toJson());
    }
}
