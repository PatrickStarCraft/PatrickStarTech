package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.api.recipe.ShapedFluidContainerRecipe;
import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ShapedFluidContainerRecipeBuilder extends ShapedRecipeBuilder {

    public ShapedFluidContainerRecipeBuilder(@Nullable Identifier id) {
        super(id);
    }

    public void save(Consumer<GeneratedRecipe> consumer) {
        consumer.accept(GeneratedRecipe.create((id == null ? defaultId() : id).withPrefix("shaped_fluid_container/"),
                ShapedFluidContainerRecipe.SERIALIZER, this::toJson));
    }
}
