package com.gregtechceu.gtceu.data.recipe.builder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

/** Codec helpers for generated item recipes; no world or mod bootstrap is needed. */
public final class RecipeBuilderCodecs {

    private RecipeBuilderCodecs() {}

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /** Tag references are serialized, never resolved into a snapshot of the tag's current contents. */
    @SuppressWarnings("deprecation")
    public static Ingredient tag(TagKey<Item> tag) {
        // Runtime-pack generation precedes tag binding. The named holder is only used for encoding.
        return Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tag));
    }

    public static Ingredient stack(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Recipe ingredient cannot be empty");
        return stack.getComponentsPatch().isEmpty() ? Ingredient.of(stack.getItem()) :
                DataComponentIngredient.of(true, stack);
    }

    public static JsonElement ingredient(Ingredient ingredient) {
        return ingredient(ingredient, registries());
    }

    public static JsonElement ingredient(Ingredient ingredient, HolderLookup.Provider registries) {
        var json = Ingredient.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), ingredient).getOrThrow();
        if (ingredient.getCustomIngredient() instanceof DataComponentIngredient componentIngredient) {
            var customData = componentIngredient.components().getPatch(DataComponents.CUSTOM_DATA);
            if (customData != null) customData.ifPresent(data -> preserveCustomData(json.getAsJsonObject(), data));
        }
        return json;
    }

    public static JsonElement result(ItemStack stack) {
        return result(stack, registries());
    }

    public static JsonElement result(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Recipe result cannot be empty");
        var json = ItemStackTemplate.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries),
                ItemStackTemplate.fromStack(stack)).getOrThrow();
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) preserveCustomData(json.getAsJsonObject(), customData);
        return json;
    }

    private static void preserveCustomData(JsonObject json, CustomData data) {
        // CustomData's codec accepts SNBT. Unlike JSON numbers, it retains byte/int/long types,
        // which strict ingredient equality and GT's LongTag capacity overrides depend on.
        var components = json.getAsJsonObject("components");
        if (components == null) {
            components = new JsonObject();
            json.add("components", components);
        }
        components.addProperty("minecraft:custom_data", data.copyTag().toString());
    }
}
