package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.api.recipe.ShapedEnergyTransferRecipe;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ShapedEnergyTransferRecipeBuilder {

    protected net.minecraft.core.HolderLookup.Provider registries =
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public ShapedEnergyTransferRecipeBuilder registries(net.minecraft.core.HolderLookup.Provider registries) {
        this.registries = java.util.Objects.requireNonNull(registries);
        return this;
    }

    protected ItemStack output = ItemStack.EMPTY;
    protected @Nullable Ingredient chargeIngredient;
    protected @Nullable Identifier id;
    protected @Nullable String group;
    protected boolean transferMaxCharge;
    protected boolean overrideCharge;

    protected List<String[]> shape = new ArrayList<>();
    protected Map<Character, Ingredient> ingredientMap = new LinkedHashMap<>();

    public ShapedEnergyTransferRecipeBuilder(@Nullable Identifier id) {
        this.id = id;
    }

    public ShapedEnergyTransferRecipeBuilder() {
        this(null);
    }

    public ShapedEnergyTransferRecipeBuilder slice(String... data) {
        this.shape.add(data);
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder where(char symbol, Ingredient value) {
        this.ingredientMap.put(symbol, value);
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder pattern(String slice) {
        return slice(slice);
    }

    public ShapedEnergyTransferRecipeBuilder define(char cha, TagKey<Item> itemStack) {
        return where(cha, RecipeBuilderCodecs.tag(itemStack));
    }

    public ShapedEnergyTransferRecipeBuilder define(char cha, ItemStack itemStack) {
        return where(cha, RecipeBuilderCodecs.stack(itemStack));
    }

    public ShapedEnergyTransferRecipeBuilder define(char cha, ItemLike itemLike) {
        return where(cha, Ingredient.of(itemLike));
    }

    public ShapedEnergyTransferRecipeBuilder define(char cha, Ingredient ingredient) {
        return where(cha, ingredient);
    }

    public ShapedEnergyTransferRecipeBuilder chargeIngredient(Ingredient chargeIngredient) {
        this.chargeIngredient = chargeIngredient;
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder overrideCharge(boolean overrideCharge) {
        this.overrideCharge = overrideCharge;
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder transferMaxCharge(boolean transferMaxCharge) {
        this.transferMaxCharge = transferMaxCharge;
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder output(ItemStack itemStack) {
        this.output = itemStack.copy();
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder output(ItemStack itemStack, int count) {
        this.output = itemStack.copy();
        this.output.setCount(count);
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder output(ItemStack itemStack, int count, DataComponentPatch components) {
        this.output = itemStack.copy();
        this.output.setCount(count);
        this.output.applyComponents(components);
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder id(Identifier id) {
        this.id = id;
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder id(String id) {
        this.id = Identifier.parse(id);
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public ShapedEnergyTransferRecipeBuilder shallowCopy() {
        var builder = new ShapedEnergyTransferRecipeBuilder();
        builder.shape = new ArrayList<>(this.shape);
        builder.ingredientMap = new HashMap<>(this.ingredientMap);
        builder.output = output.copy();
        builder.registries = registries;
        return builder;
    }

    public void toJson(JsonObject json) {
        if (group != null) {
            json.addProperty("group", group);
        }

        if (!shape.isEmpty()) {
            JsonArray pattern = new JsonArray();
            for (String[] strings : shape) {
                for (String string : strings) {
                    pattern.add(string);
                }
            }
            json.add("pattern", pattern);
        }

        if (!ingredientMap.isEmpty()) {
            JsonObject key = new JsonObject();
            ingredientMap.forEach((k, v) -> key.add(k.toString(), RecipeBuilderCodecs.ingredient(v, registries)));
            json.add("key", key);
        }

        json.addProperty("overrideCharge", overrideCharge);
        json.addProperty("transferMaxCharge", transferMaxCharge);
        if (chargeIngredient == null) {
            com.mojang.logging.LogUtils.getLogger().error("shaped energy transfer recipe {} chargeIngredient is empty", id);
            throw new IllegalArgumentException(id + ": chargeIngredient is empty");
        } else {
            json.add("chargeIngredient", RecipeBuilderCodecs.ingredient(chargeIngredient, registries));
        }
        if (output.isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().error("shaped energy transfer recipe {} output is empty", id);
            throw new IllegalArgumentException(id + ": output items is empty");
        } else {
            json.add("result", RecipeBuilderCodecs.result(output, registries));
        }
    }

    protected Identifier defaultId() {
        return BuiltInRegistries.ITEM.getKey(output.getItem());
    }

    public void save(Consumer<GeneratedRecipe> consumer) {
        consumer.accept(GeneratedRecipe.create((id == null ? defaultId() : id).withPrefix("shaped/"),
                ShapedEnergyTransferRecipe.SERIALIZER, this::toJson));
    }
}
