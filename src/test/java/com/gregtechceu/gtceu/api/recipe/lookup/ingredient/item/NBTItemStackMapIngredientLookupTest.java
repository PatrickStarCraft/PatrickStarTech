package com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeAdditionHandler;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeDB;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Predicate;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class NBTItemStackMapIngredientLookupTest {

    private static RecipeDB DB;
    private static final Predicate<GTRecipe> ALWAYS_TRUE = gtRecipe -> true;
    private static GTRecipe PARTIAL, STRICT;
    private static CompoundTag tag1, tag2;

    @BeforeBatch(batch = "NBTItemStackMapIngredientLookup")
    public static void prepare(ServerLevel level) {
        GTRecipeType recipeType = TestUtils.createRecipeType("component_item_stack_map_ingredient_lookup");
        RecipeAdditionHandler handler = recipeType.getAdditionHandler();
        DB = recipeType.db();

        tag1 = new CompoundTag();
        tag1.putString("tag1", "tag1");
        tag2 = tag1.copy();
        tag2.putBoolean("extra", true);

        PARTIAL = recipeType.recipeBuilder("partial_component_match")
                .inputItems(partial(Items.RED_BED, tag1))
                .outputItems(Items.RED_BED, 1)
                .buildRawRecipe();
        STRICT = recipeType.recipeBuilder("strict_component_match")
                .inputItems(strict(Items.BLUE_BED, tag1))
                .outputItems(Items.BLUE_BED, 1)
                .buildRawRecipe();

        handler.beginStaging();
        handler.addStaging(PARTIAL);
        handler.addStaging(STRICT);
        handler.completeStaging();
    }

    private static DataComponentPatch patch(CompoundTag tag) {
        return DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build();
    }

    private static Ingredient partial(Item item, CompoundTag tag) {
        return DataComponentIngredient.of(false, patch(tag), item);
    }

    private static Ingredient strict(Item item, CompoundTag tag) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return DataComponentIngredient.of(true, stack);
    }

    private static List<AbstractMapIngredient> lookup(Ingredient ingredient) {
        return MapIngredientTypeManager.getFrom(ingredient, ItemRecipeCapability.CAP);
    }

    @GameTest(template = "empty", batch = "NBTItemStackMapIngredientLookup")
    public static void componentIngredientLookupPreservesPartialAndStrictMatching(GameTestHelper helper) {
        ItemStack red = new ItemStack(Items.RED_BED);
        red.set(DataComponents.CUSTOM_DATA, CustomData.of(tag1));
        red.set(DataComponents.CUSTOM_NAME, Component.literal("Extra component"));
        GTRecipe result = DB.find(MapIngredientTypeManager.getFrom(red, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == PARTIAL, "nonexhaustive components should accept an additional component type");

        ItemStack blue = new ItemStack(Items.BLUE_BED);
        blue.set(DataComponents.CUSTOM_DATA, CustomData.of(tag1));
        result = DB.find(MapIngredientTypeManager.getFrom(blue, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == STRICT, "strict component ingredient should accept an exact component set");

        ItemStack green = new ItemStack(Items.GREEN_BED);
        green.set(DataComponents.CUSTOM_DATA, CustomData.of(tag1));
        result = DB.find(MapIngredientTypeManager.getFrom(green, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == null, "a different item should not match the strict component ingredient");

        blue.set(DataComponents.CUSTOM_NAME, Component.literal("Extra component"));
        result = DB.find(MapIngredientTypeManager.getFrom(blue, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == null, "strict component ingredient should reject extra components");

        red.set(DataComponents.CUSTOM_DATA, CustomData.of(tag2));
        result = DB.find(MapIngredientTypeManager.getFrom(red, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == null, "nonexhaustive matching still requires equality of each specified component");

        ItemStack stack = IngredientStacks.getItems(partial(Items.RED_BED, tag1))[0].copyWithCount(32);
        result = DB.find(MapIngredientTypeManager.getFrom(stack, ItemRecipeCapability.CAP), ALWAYS_TRUE);
        helper.assertTrue(result == PARTIAL, "stack count should not affect component ingredient lookup");

        result = DB.find(lookup(SizedIngredient.create(partial(Items.RED_BED, tag1), 32)), ALWAYS_TRUE);
        helper.assertTrue(result == PARTIAL, "sized ingredient counts should not affect item lookup keys");
        helper.succeed();
    }
}
