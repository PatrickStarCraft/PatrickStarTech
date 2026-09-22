package com.gregtechceu.gtceu.api.recipe.content;

import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.EmptyIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;

public class SerializerIngredient implements IContentSerializer<Ingredient> {

    public static final Codec<Ingredient> CODEC = Ingredient.CODEC;

    public static SerializerIngredient INSTANCE = new SerializerIngredient();

    private SerializerIngredient() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Ingredient content) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(registryBuffer(buf), content);
    }

    @Override
    public Ingredient fromNetwork(FriendlyByteBuf buf) {
        return Ingredient.CONTENTS_STREAM_CODEC.decode(registryBuffer(buf));
    }

    private static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buf) {
        return buf instanceof RegistryFriendlyByteBuf registryBuf ? registryBuf :
                new RegistryFriendlyByteBuf(buf, GTRegistries.builtinRegistry());
    }

    @Override
    public Ingredient fromJson(JsonElement json) {
        return fromJson(json, GTRegistries.builtinRegistry());
    }

    @Override
    public JsonElement toJson(Ingredient content) {
        return toJson(content, GTRegistries.builtinRegistry());
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Ingredient of(Object o) {
        if (o instanceof Ingredient ingredient) {
            return ingredient;
        } else if (o instanceof net.neoforged.neoforge.common.crafting.ICustomIngredient custom) {
            return custom.toVanilla();
        } else if (o instanceof ItemStack itemStack) {
            return SizedIngredient.create(itemStack);
        } else if (o instanceof ItemLike itemLike) {
            return Ingredient.of(itemLike);
        } else if (o instanceof TagKey tag) {
            return com.gregtechceu.gtceu.data.recipe.builder.RecipeBuilderCodecs.tag(tag);
        }
        return EmptyIngredient.VANILLA;
    }

    @Override
    public Ingredient defaultValue() {
        return EmptyIngredient.VANILLA;
    }

    @Override
    public Class<Ingredient> contentClass() {
        return Ingredient.class;
    }

    @Override
    public Codec<Ingredient> codec() {
        return CODEC;
    }
}
