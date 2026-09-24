package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

/** Recipe payload fixture; RecipeDB and its branches remain the production implementation. */
public final class GTRecipe {

    private final Identifier id;
    private final RecipeType<?> type;
    public final RecipeCategory recipeCategory = new RecipeCategory();
    public final int duration = 1;

    public GTRecipe(Identifier id, RecipeType<?> type) {
        this.id = id;
        this.type = type;
    }

    public Identifier getId() {
        return id;
    }

    public RecipeType<?> getType() {
        return type;
    }

    public List<Content> getInputContents(RecipeCapability<?> capability) {
        return List.of();
    }

    public static final class RecipeCategory {

        private final List<GTRecipe> recipes = new ArrayList<>();

        public void addRecipe(GTRecipe recipe) {
            recipes.add(recipe);
        }
    }
}
