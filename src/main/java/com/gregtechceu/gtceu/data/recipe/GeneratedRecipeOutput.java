package com.gregtechceu.gtceu.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.WithConditions;

import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/** Adapts modern vanilla builders to GT's JSON-resource pipeline using the supplied registry context. */
public record GeneratedRecipeOutput(HolderLookup.Provider registries,
                                    Consumer<GeneratedRecipe> consumer) implements RecipeOutput {

    @Override
    public void accept(ResourceKey<Recipe<?>> key, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                       ICondition... conditions) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var json = Recipe.CONDITIONAL_CODEC.encodeStart(ops, Optional.of(new WithConditions<>(recipe, conditions)))
                .getOrThrow().getAsJsonObject();
        var advancementJson = advancement == null ? null : Advancement.CONDITIONAL_CODEC.encodeStart(ops,
                Optional.of(new WithConditions<>(advancement.value(), conditions))).getOrThrow().getAsJsonObject();
        consumer.accept(new GeneratedRecipe(key.identifier(), json,
                advancement == null ? null : advancement.id(), advancementJson));
    }

    @Override
    public Advancement.Builder advancement() {
        return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
    }

    @Override
    public void includeRootAdvancement() {
        // This pack adds recipes to vanilla's existing tree rather than replacing its root.
        throw new UnsupportedOperationException("GT's runtime pack uses the vanilla recipe advancement root");
    }
}
