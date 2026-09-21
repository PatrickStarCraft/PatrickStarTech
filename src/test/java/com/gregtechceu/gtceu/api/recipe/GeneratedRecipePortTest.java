package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;
import com.gregtechceu.gtceu.data.recipe.GeneratedRecipeOutput;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.neoforge.common.conditions.AlwaysCondition;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedRecipePortTest {

    @BeforeAll
    static void setup() {
        for (var item : List.of(Items.STONE, Items.DIRT, Items.STICK, Items.DIAMOND)) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("gtceu", path); }

    @Test
    void snapshotsProtectRecipeAndAdvancementData() {
        var recipe = new JsonObject();
        var nested = new JsonObject();
        nested.addProperty("count", 2);
        recipe.add("result", nested);
        var advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");
        var generated = new GeneratedRecipe(id("test"), recipe, id("adv"), advancement);
        nested.addProperty("count", 99);
        generated.recipeJson().getAsJsonObject("result").addProperty("count", 100);
        advancement.addProperty("parent", "other");
        generated.advancementJson().addProperty("parent", "also_other");
        assertEquals(2, generated.recipeJson().getAsJsonObject("result").get("count").getAsInt());
        assertEquals("minecraft:recipes/root", generated.advancementJson().get("parent").getAsString());
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedRecipe(id("bad"), recipe, id("adv"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedRecipe(id("bad"), recipe, null, advancement));
    }

    @Test
    void selectedSerializerAndModernResourcePathsArePreserved() {
        var generated = GeneratedRecipe.create(id("shaped/test"), ShapedRecipe.SERIALIZER, json -> {
            json.addProperty("type", "incorrect:dispatch");
            json.addProperty("group", "test-group");
        });
        assertEquals("minecraft:crafting_shaped", generated.recipeJson().get("type").getAsString());
        assertEquals("test-group", generated.recipeJson().get("group").getAsString());
        assertNull(generated.advancementJson());
        assertEquals(id("recipe/shaped/test.json"), GeneratedRecipe.RECIPES.idToFile(generated.id()));
        assertEquals(id("advancement/recipes/test.json"), GeneratedRecipe.ADVANCEMENTS.idToFile(id("recipes/test")));
    }

    @Test
    void vanillaBuilderEmitsDecodableRecipeAndUnlockAdvancement() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var emitted = new ArrayList<GeneratedRecipe>();
        var output = new GeneratedRecipeOutput(registries, emitted::add);
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(Items.STONE), Ingredient.of(Items.DIRT),
                Ingredient.of(Items.STICK), RecipeCategory.MISC, Items.DIAMOND)
                .unlocks("has_stone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STONE))
                .save(output, ResourceKey.create(Registries.RECIPE, id("smithing/test")));
        assertEquals(1, emitted.size());
        var generated = emitted.getFirst();
        assertEquals(id("smithing/test"), generated.id());
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        assertInstanceOf(SmithingTransformRecipe.class, Recipe.CODEC.parse(ops, generated.recipeJson()).getOrThrow());
        assertNotNull(generated.advancementId());
        var advancement = Advancement.CODEC.parse(ops, generated.advancementJson()).getOrThrow();
        assertTrue(advancement.criteria().containsKey("has_stone"));
        assertEquals(Identifier.parse("minecraft:recipes/root"), advancement.parent().orElseThrow());
    }

    @Test
    void conditionsReachBothRecipeAndAdvancement() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var emitted = new ArrayList<GeneratedRecipe>();
        var output = new GeneratedRecipeOutput(registries, emitted::add);
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(Items.STONE), Ingredient.of(Items.DIRT),
                Ingredient.of(Items.STICK), RecipeCategory.MISC, Items.DIAMOND)
                .unlocks("has_stone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STONE))
                .save(output.withConditions(AlwaysCondition.INSTANCE),
                        ResourceKey.create(Registries.RECIPE, id("conditional")));
        var generated = emitted.getFirst();
        assertEquals(1, generated.recipeJson().getAsJsonArray("neoforge:conditions").size());
        assertEquals(generated.recipeJson().get("neoforge:conditions"),
                generated.advancementJson().get("neoforge:conditions"));
        assertThrows(UnsupportedOperationException.class, output::includeRootAdvancement);
    }
}
