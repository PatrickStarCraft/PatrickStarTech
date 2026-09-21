package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Accessors(chain = true, fluent = true)
public class ShapelessRecipeBuilder {

    private final List<Ingredient> ingredients = new ArrayList<>();
    @Setter
    protected String group;

    protected net.minecraft.core.HolderLookup.Provider registries =
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public ShapelessRecipeBuilder registries(net.minecraft.core.HolderLookup.Provider registries) {
        this.registries = java.util.Objects.requireNonNull(registries);
        return this;
    }

    private ItemStack output = ItemStack.EMPTY;
    @Setter
    private float experience;
    @Setter
    private int cookingTime;
    @Setter
    protected Identifier id;

    public ShapelessRecipeBuilder(@Nullable Identifier id) {
        this.id = id;
    }

    public ShapelessRecipeBuilder requires(TagKey<Item> itemStack) {
        return requires(RecipeBuilderCodecs.tag(itemStack));
    }

    public ShapelessRecipeBuilder requires(ItemStack itemStack) {
        requires(RecipeBuilderCodecs.stack(itemStack));
        return this;
    }

    public ShapelessRecipeBuilder requires(ItemLike itemLike) {
        return requires(Ingredient.of(itemLike));
    }

    public ShapelessRecipeBuilder requires(Ingredient ingredient) {
        ingredients.add(ingredient);
        return this;
    }

    public ShapelessRecipeBuilder output(ItemStack itemStack) {
        this.output = itemStack.copy();
        return this;
    }

    public ShapelessRecipeBuilder output(ItemStack itemStack, int count) {
        this.output = itemStack.copy();
        this.output.setCount(count);
        return this;
    }

    public ShapelessRecipeBuilder output(ItemStack itemStack, int count, DataComponentPatch components) {
        this.output = itemStack.copy();
        this.output.setCount(count);
        this.output.applyComponents(components);
        return this;
    }

    protected Identifier defaultId() {
        return BuiltInRegistries.ITEM.getKey(output.getItem());
    }

    public void toJson(JsonObject json) {
        if (group != null) {
            json.addProperty("group", group);
        }

        JsonArray jsonarray = new JsonArray();
        for (Ingredient ingredient : ingredients) {
            jsonarray.add(RecipeBuilderCodecs.ingredient(ingredient, registries));
        }
        json.add("ingredients", jsonarray);

        if (output.isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().error("shapeless recipe {} output is empty", id);
            throw new IllegalArgumentException(id + ": output items is empty");
        } else {
            json.add("result", RecipeBuilderCodecs.result(output, registries));
        }
    }

    public void save(Consumer<GeneratedRecipe> consumer) {
        consumer.accept(GeneratedRecipe.create((id == null ? defaultId() : id).withPrefix("shapeless/"),
                net.minecraft.world.item.crafting.ShapelessRecipe.SERIALIZER, this::toJson));
    }
}
