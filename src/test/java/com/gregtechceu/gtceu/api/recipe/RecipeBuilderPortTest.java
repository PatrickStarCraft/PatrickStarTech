package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.ElectricItemCapabilities;
import com.gregtechceu.gtceu.api.item.capability.ElectricItem;
import com.gregtechceu.gtceu.api.item.data.ElectricItemData;
import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;
import com.gregtechceu.gtceu.data.recipe.builder.RecipeBuilderCodecs;
import com.gregtechceu.gtceu.data.recipe.builder.ShapedRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.ShapedEnergyTransferRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.ShapelessRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.SimpleCookingRecipeBuilder;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeBuilderPortTest {

    private static RegistryAccess registries;

    @BeforeAll
    static void setup() throws Exception {
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        for (var item : List.of(Items.STONE, Items.DIRT, Items.DIAMOND, Items.STICK)) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
        var constructor = RegisterCapabilitiesEvent.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ElectricItemCapabilities.register(constructor.newInstance(), Items.STICK,
                stack -> new ElectricItem(stack, 1000, 1, true, true));
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("gtceu", path); }

    private static ItemStack output() {
        var stack = new ItemStack(Items.DIAMOND, 2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Port result"));
        var data = new CompoundTag();
        data.putLong("MaxCharge", 1000);
        data.putInt("exactInt", 4);
        data.putByte("exactByte", (byte) 3);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    @Test
    void resultCodecPreservesComponentsCountsAndNbtTypes() {
        var expected = output();
        var json = RecipeBuilderCodecs.result(expected, registries);
        var template = ItemStackTemplate.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), json).getOrThrow();
        assertTrue(ItemStack.matches(expected, template.create()));
        assertEquals(1000, ElectricItemData.getMaxCharge(template.create(), -1));
        assertThrows(IllegalArgumentException.class, () -> RecipeBuilderCodecs.result(ItemStack.EMPTY));
    }

    @Test
    void strictIngredientRoundTripRejectsDifferentAndExtraComponents() {
        var expected = output();
        var encoded = RecipeBuilderCodecs.ingredient(RecipeBuilderCodecs.stack(expected), registries);
        var ingredient = Ingredient.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), encoded).getOrThrow();
        assertTrue(ingredient.test(expected.copyWithCount(1)));
        assertFalse(ingredient.test(new ItemStack(Items.DIAMOND)));
        var changed = expected.copy();
        changed.set(DataComponents.CUSTOM_NAME, Component.literal("Different"));
        assertFalse(ingredient.test(changed));
        changed = expected.copy();
        changed.set(DataComponents.REPAIR_COST, 1);
        assertFalse(ingredient.test(changed));
        assertTrue(RecipeBuilderCodecs.stack(new ItemStack(Items.DIAMOND)).test(expected));
        assertThrows(IllegalArgumentException.class, () -> RecipeBuilderCodecs.stack(ItemStack.EMPTY));
    }

    @Test
    void unboundTagsSerializeWithoutResolvingTheirContents() {
        var tag = TagKey.create(Registries.ITEM, id("unbound_builder_test"));
        var json = RecipeBuilderCodecs.ingredient(RecipeBuilderCodecs.tag(tag), registries);
        assertEquals("#gtceu:unbound_builder_test", json.getAsString());
        var builder = new ShapedRecipeBuilder(id("tag_test")).pattern("A").define('A', tag).output(output());
        var body = new JsonObject();
        builder.toJson(body);
        assertEquals(json, body.getAsJsonObject("key").get("A"));
    }

    @Test
    void shapedAndShapelessBuildersEmitLoadableRecipesWithIndependentOutputs() {
        var emitted = new ArrayList<GeneratedRecipe>();
        new ShapedRecipeBuilder(id("shaped_test")).pattern("A").define('A', Items.STONE).output(output())
                .save(emitted::add);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var shaped = assertInstanceOf(ShapedRecipe.class, Recipe.CODEC.parse(ops, emitted.getFirst().recipeJson()).getOrThrow());
        var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STONE)));
        assertTrue(shaped.matches(input, null));
        var first = shaped.assemble(input);
        assertTrue(ItemStack.matches(output(), first));
        first.setCount(1);
        assertEquals(2, shaped.assemble(input).getCount());
        emitted.clear();
        new ShapelessRecipeBuilder(id("shapeless_test")).requires(Items.STONE).output(output()).save(emitted::add);
        var shapeless = assertInstanceOf(ShapelessRecipe.class, Recipe.CODEC.parse(ops, emitted.getFirst().recipeJson()).getOrThrow());
        assertTrue(shapeless.matches(input, null));
        assertTrue(ItemStack.matches(output(), shapeless.assemble(input)));
    }

    @Test
    void strictBuilderRetainsPatternAndComponentPatchOutput() {
        var patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Patched")).build();
        var builder = new ShapedRecipeBuilder(id("strict_test")).pattern(" A").define('A', Items.STONE)
                .matchSize(true).output(new ItemStack(Items.DIAMOND), 3, patch);
        var body = new JsonObject();
        builder.toJson(body);
        var recipe = StrictShapedRecipe.MAP_CODEC.codec().parse(RegistryOps.create(JsonOps.INSTANCE, registries), body).getOrThrow();
        var input = CraftingInput.of(2, 1, List.of(ItemStack.EMPTY, new ItemStack(Items.STONE)));
        assertTrue(recipe.matches(input, null));
        assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.STONE))), null));
        assertEquals(3, recipe.assemble(input).getCount());
        assertEquals(Component.literal("Patched"), recipe.assemble(input).get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void allCookingBuildersRoundTripConfiguredResultAndTime() {
        for (var builder : List.of(SimpleCookingRecipeBuilder.smelting(id("cook")),
                SimpleCookingRecipeBuilder.blasting(id("cook")), SimpleCookingRecipeBuilder.smoking(id("cook")),
                SimpleCookingRecipeBuilder.campfireCooking(id("cook")))) {
            var emitted = new ArrayList<GeneratedRecipe>();
            builder.input(Items.STONE).output(output()).cookingTime(80).experience(0.5f)
                    .category(CookingBookCategory.FOOD).save(emitted::add);
            var recipe = assertInstanceOf(AbstractCookingRecipe.class, Recipe.CODEC.parse(
                    RegistryOps.create(JsonOps.INSTANCE, registries), emitted.getFirst().recipeJson()).getOrThrow());
            assertEquals(80, recipe.cookingTime());
            assertEquals(0.5f, recipe.experience());
            assertTrue(ItemStack.matches(output(), recipe.assemble(new SingleRecipeInput(new ItemStack(Items.STONE)))));
            assertEquals("food", emitted.getFirst().recipeJson().get("category").getAsString());
        }
    }

    @Test
    void energyCraftingUsesLiveInputChargeWithoutMutatingIt() {
        var builder = new ShapedEnergyTransferRecipeBuilder(id("energy_test")).pattern("A")
                .define('A', Items.STICK).chargeIngredient(Ingredient.of(Items.STICK)).output(output());
        var body = new JsonObject();
        builder.toJson(body);
        var recipe = ShapedEnergyTransferRecipe.MAP_CODEC.codec().parse(
                RegistryOps.create(JsonOps.INSTANCE, registries), body).getOrThrow();
        var battery = new ItemStack(Items.STICK);
        battery.getCapability(ElectricItemCapabilities.ELECTRIC_ITEM).charge(250, 1, true, false);
        var input = CraftingInput.of(1, 1, List.of(battery));
        assertTrue(recipe.matches(input, null));
        var result = recipe.assemble(input);
        assertEquals(250, ElectricItemData.getCharge(result, 1000));
        assertEquals(1000, ElectricItemData.getMaxCharge(result, -1));
        assertEquals(250, ElectricItemData.getCharge(battery, 1000));
        assertEquals(Component.literal("Port result"), result.get(DataComponents.CUSTOM_NAME));
        ElectricItemData.setCharge(result, 1);
        assertEquals(250, ElectricItemData.getCharge(recipe.assemble(input), 1000));
    }
}
