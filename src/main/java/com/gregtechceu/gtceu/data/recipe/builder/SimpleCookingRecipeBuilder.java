package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;

import com.google.gson.JsonObject;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Accessors(chain = true, fluent = true)
@SuppressWarnings("deprecation")
public class SimpleCookingRecipeBuilder<T extends AbstractCookingRecipe> {

    protected final String folder;
    protected final RecipeSerializer<T> serializer;
    protected @Nullable Ingredient input;
    @Setter
    protected @Nullable String group;
    @Setter
    protected CookingBookCategory category = CookingBookCategory.MISC;

    protected net.minecraft.core.HolderLookup.Provider registries =
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public SimpleCookingRecipeBuilder<T> registries(net.minecraft.core.HolderLookup.Provider registries) {
        this.registries = java.util.Objects.requireNonNull(registries);
        return this;
    }

    protected ItemStack output = ItemStack.EMPTY;
    @Setter
    protected float experience;
    @Setter
    protected int cookingTime;
    @Setter
    protected @Nullable Identifier id;

    protected SimpleCookingRecipeBuilder(@Nullable Identifier id, String folder, RecipeSerializer<T> serializer) {
        this.id = id;
        this.folder = folder;
        this.serializer = serializer;
    }

    public static SimpleCookingRecipeBuilder<CampfireCookingRecipe> campfireCooking(@Nullable Identifier id) {
        return new SimpleCookingRecipeBuilder<>(id, "campfire_cooking", CampfireCookingRecipe.SERIALIZER);
    }

    public static SimpleCookingRecipeBuilder<SmeltingRecipe> smelting(@Nullable Identifier id) {
        return new SimpleCookingRecipeBuilder<>(id, "smelting", SmeltingRecipe.SERIALIZER);
    }

    public static SimpleCookingRecipeBuilder<BlastingRecipe> blasting(@Nullable Identifier id) {
        return new SimpleCookingRecipeBuilder<>(id, "blasting", BlastingRecipe.SERIALIZER);
    }

    public static SimpleCookingRecipeBuilder<SmokingRecipe> smoking(@Nullable Identifier id) {
        return new SimpleCookingRecipeBuilder<>(id, "smoking", SmokingRecipe.SERIALIZER);
    }

    public SimpleCookingRecipeBuilder<T> input(TagKey<Item> tag) {
        return input(RecipeBuilderCodecs.tag(tag));
    }

    public SimpleCookingRecipeBuilder<T> input(ItemStack itemStack) {
        input = RecipeBuilderCodecs.stack(itemStack);
        return this;
    }

    public SimpleCookingRecipeBuilder<T> input(ItemLike itemLike) {
        return input(Ingredient.of(itemLike));
    }

    public SimpleCookingRecipeBuilder<T> input(Ingredient ingredient) {
        input = ingredient;
        return this;
    }

    public SimpleCookingRecipeBuilder<T> output(ItemStack itemStack) {
        this.output = itemStack.copy();
        return this;
    }

    public SimpleCookingRecipeBuilder<T> output(ItemStack itemStack, int count) {
        this.output = itemStack.copyWithCount(count);
        return this;
    }

    protected Identifier defaultId() {
        return BuiltInRegistries.ITEM.getKey(output.getItem());
    }

    public void toJson(JsonObject json) {
        if (group != null) {
            json.addProperty("group", group);
        }

        if (input == null) {
            com.mojang.logging.LogUtils.getLogger().error("{} recipe {} input is empty", folder, id);
            throw new IllegalArgumentException(id + ": input item is empty");
        }
        if (output.isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().error("{} recipe {} output is empty", folder, id);
            throw new IllegalArgumentException(id + ": output item is empty");
        }

        json.add("ingredient", RecipeBuilderCodecs.ingredient(input, registries));

        json.add("result", RecipeBuilderCodecs.result(output, registries));

        json.addProperty("experience", experience);
        json.addProperty("category", category.getSerializedName());
        json.addProperty("cookingtime", cookingTime);
    }

    public void save(Consumer<GeneratedRecipe> consumer) {
        Identifier recipeId = (id == null ? defaultId() : id).withPrefix(folder + "/");

        consumer.accept(GeneratedRecipe.create(recipeId, serializer, this::toJson));
    }
}
