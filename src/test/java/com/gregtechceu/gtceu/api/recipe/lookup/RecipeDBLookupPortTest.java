package com.gregtechceu.gtceu.api.recipe.lookup;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.DataComponentItemStackMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.IntersectionMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.ItemStackMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.ItemTagMapIngredient;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.Lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDBLookupPortTest {

    @BeforeAll
    static void bindItemsAndMapKeys() {
        for (var item : new net.minecraft.world.item.Item[] { Items.BRICK, Items.COBBLESTONE, Items.PAPER }) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
        MapIngredientTypeManager.registerMapIngredient(
                DataComponentIngredient.class, DataComponentItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                CompoundIngredient.class, ItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                IntersectionIngredient.class, IntersectionMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                Ingredient.class, ItemTagMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                Ingredient.class, ItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                ItemStack.class, DataComponentItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                ItemStack.class, ItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                ItemStack.class, ItemTagMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                ItemStack.class, IntersectionMapIngredient::from);
    }

    @Test
    void publicInputLookupFindsOnlyMatchingItemKeysAndAppliesItsPredicate() {
        RecipeDB database = new RecipeDB();
        GTRecipe brickRecipe = recipe("brick_input");
        assertTrue(database.add(brickRecipe, keys(new ItemStack(Items.BRICK))));

        assertSame(brickRecipe, database.find(inputs(new ItemStack(Items.BRICK)), recipe -> true));
        assertNull(database.find(inputs(new ItemStack(Items.COBBLESTONE)), recipe -> true));
        assertNull(database.find(inputs(new ItemStack(Items.BRICK)), recipe -> false));
    }

    @Test
    void multipleRequiredKeysCanBeSearchedInEitherInputOrder() {
        RecipeDB database = new RecipeDB();
        GTRecipe recipe = recipe("brick_and_paper");
        List<List<AbstractMapIngredient>> recipeKeys = concat(
                keys(new ItemStack(Items.BRICK)), keys(new ItemStack(Items.PAPER)));
        assertTrue(database.add(recipe, recipeKeys));

        assertSame(recipe, database.find(inputs(new ItemStack(Items.BRICK), new ItemStack(Items.PAPER)), r -> true));
        assertSame(recipe, database.find(inputs(new ItemStack(Items.PAPER), new ItemStack(Items.BRICK)), r -> true));
        assertNull(database.find(inputs(new ItemStack(Items.BRICK)), r -> true));
    }

    @Test
    void componentIngredientKeysReachTheDatabaseAndUseTheirMatchingContract() {
        RecipeDB database = new RecipeDB();
        CompoundTag requestedData = new CompoundTag();
        requestedData.putString("grade", "treated");
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(requestedData))
                .build();
        Ingredient ingredient = DataComponentIngredient.of(false, patch, Items.BRICK);
        GTRecipe recipe = recipe("component_brick");
        assertTrue(database.add(recipe, List.of(MapIngredientTypeManager.getFrom(
                ingredient, ItemRecipeCapability.CAP))));

        ItemStack matching = new ItemStack(Items.BRICK);
        matching.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(requestedData));
        matching.set(DataComponents.CUSTOM_NAME, Component.literal("additional component type"));
        assertSame(recipe, database.find(inputs(matching), r -> true));

        CompoundTag differentData = requestedData.copy();
        differentData.putBoolean("extra", true);
        ItemStack mismatched = new ItemStack(Items.BRICK);
        mismatched.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(differentData));
        assertNull(database.find(inputs(mismatched), r -> true),
                "partial component matching still compares its specified CUSTOM_DATA value exactly");
    }

    @Test
    void compoundIngredientIndexesEveryAlternative() {
        RecipeDB database = new RecipeDB();
        Ingredient ingredient = CompoundIngredient.of(
                Ingredient.of(Items.BRICK), Ingredient.of(Items.PAPER));
        GTRecipe recipe = recipe("compound_item_input");
        assertTrue(database.add(recipe, List.of(MapIngredientTypeManager.getFrom(
                ingredient, ItemRecipeCapability.CAP))));

        assertSame(recipe, database.find(inputs(new ItemStack(Items.BRICK)), r -> true));
        assertSame(recipe, database.find(inputs(new ItemStack(Items.PAPER)), r -> true));
        assertNull(database.find(inputs(new ItemStack(Items.COBBLESTONE)), r -> true));
    }

    @Test
    void intersectionIngredientIndexesAStackAcceptedByEveryChild() {
        RecipeDB database = new RecipeDB();
        Ingredient ingredient = IntersectionIngredient.of(
                Ingredient.of(Items.BRICK), Ingredient.of(Items.BRICK));
        ItemStack candidate = new ItemStack(Items.BRICK);
        assertTrue(ingredient.test(candidate), "the target IntersectionIngredient contract is all children");
        List<AbstractMapIngredient> intersectionKeys =
                MapIngredientTypeManager.getFrom(ingredient, ItemRecipeCapability.CAP);
        var stackKey = MapIngredientTypeManager.getFrom(candidate, ItemRecipeCapability.CAP).stream()
                .filter(ItemStackMapIngredient.class::isInstance)
                .map(ItemStackMapIngredient.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(intersectionKeys.stream().anyMatch(key -> key.equals(stackKey) &&
                stackKey.equals(key) && key.hashCode() == stackKey.hashCode()),
                "an accepted intersection representative must be indexed with its candidate item key");
        var compositeKey = new IntersectionMapIngredient(new ArrayList<>(List.of(stackKey, stackKey)));
        assertAll("composite intersection keys retain symmetric item equality and hashes",
                () -> assertTrue(compositeKey.equals(stackKey)),
                () -> assertTrue(stackKey.equals(compositeKey)),
                () -> assertEquals(compositeKey.hashCode(), stackKey.hashCode()));
        GTRecipe recipe = recipe("intersection_item_input");
        assertTrue(database.add(recipe, List.of(intersectionKeys)));

        assertSame(recipe, database.find(inputs(candidate), r -> true));
    }

    @Test
    void intersectionOfTagsIndexesCandidatesAndThePredicateChecksEveryTag() {
        MappedRegistry<Item> itemRegistry = new MappedRegistry<>(Registries.ITEM, Lifecycle.stable());
        var brickHolder = itemRegistry.register(
                ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "intersection_brick")),
                Items.BRICK, RegistrationInfo.BUILT_IN);
        var paperHolder = itemRegistry.register(
                ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "intersection_paper")),
                Items.PAPER, RegistrationInfo.BUILT_IN);
        brickHolder.bindComponents(Items.BRICK.builtInRegistryHolder().components());
        paperHolder.bindComponents(Items.PAPER.builtInRegistryHolder().components());
        TagKey<Item> firstTag = TagKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "first_intersection_tag"));
        TagKey<Item> secondTag = TagKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "second_intersection_tag"));
        itemRegistry.bindTags(Map.of(firstTag, List.of(brickHolder, paperHolder), secondTag, List.of(brickHolder)));
        Registry<Item> registry = itemRegistry.freeze();
        Ingredient ingredient = IntersectionIngredient.of(
                Ingredient.of(registry.get(firstTag).orElseThrow()),
                Ingredient.of(registry.get(secondTag).orElseThrow()));
        ItemStack brick = new ItemStack(brickHolder);
        ItemStack paper = new ItemStack(paperHolder);
        assertTrue(ingredient.test(brick));
        assertFalse(ingredient.test(paper));

        RecipeDB database = new RecipeDB();
        GTRecipe recipe = recipe("intersection_tag_input");
        List<AbstractMapIngredient> recipeKeys = MapIngredientTypeManager.getFrom(
                ingredient, ItemRecipeCapability.CAP);
        List<AbstractMapIngredient> candidateKeys = MapIngredientTypeManager.getFrom(brick, ItemRecipeCapability.CAP);
        assertTrue(recipeKeys.stream().anyMatch(key -> candidateKeys.stream().anyMatch(key::equals)),
                () -> "intersection recipe keys " + recipeKeys + " do not overlap candidate keys " + candidateKeys);
        assertTrue(database.add(recipe, List.of(recipeKeys)));

        assertSame(recipe, database.find(inputs(brick), ignored -> ingredient.test(brick)));
        assertNull(database.find(inputs(paper), ignored -> ingredient.test(paper)));
    }

    @Test
    void tagIngredientFindsOnlyItemsBoundToItsRegistryTag() {
        MappedRegistry<Item> itemRegistry = new MappedRegistry<>(Registries.ITEM, Lifecycle.stable());
        var brickHolder = itemRegistry.register(
                ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "brick")),
                Items.BRICK, RegistrationInfo.BUILT_IN);
        var paperHolder = itemRegistry.register(
                ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "paper")),
                Items.PAPER, RegistrationInfo.BUILT_IN);
        brickHolder.bindComponents(Items.BRICK.builtInRegistryHolder().components());
        paperHolder.bindComponents(Items.PAPER.builtInRegistryHolder().components());
        TagKey<Item> inputTag = TagKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath("gtceu_test", "tagged_brick"));
        itemRegistry.bindTags(Map.of(inputTag, List.of(brickHolder)));
        Registry<Item> registry = itemRegistry.freeze();
        HolderSet.Named<Item> tagValues = registry.get(inputTag).orElseThrow();

        Ingredient ingredient = Ingredient.of(tagValues);
        ItemStack brick = new ItemStack(brickHolder);
        ItemStack paper = new ItemStack(paperHolder);
        assertTrue(ingredient.test(brick));
        assertFalse(ingredient.test(paper));

        RecipeDB database = new RecipeDB();
        GTRecipe recipe = recipe("tagged_item_input");
        assertTrue(database.add(recipe, List.of(MapIngredientTypeManager.getFrom(
                ingredient, ItemRecipeCapability.CAP))));
        assertSame(recipe, database.find(inputs(brick), r -> true));
        assertNull(database.find(inputs(paper), r -> true));
    }

    private static GTRecipe recipe(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("gtceu_test", path);
        return new GTRecipe(id, RecipeTypeFactory.create(id));
    }

    private static List<List<AbstractMapIngredient>> keys(ItemStack stack) {
        return List.of(MapIngredientTypeManager.getFrom(stack, ItemRecipeCapability.CAP));
    }

    @SafeVarargs
    private static List<List<AbstractMapIngredient>> concat(List<List<AbstractMapIngredient>>... inputs) {
        return java.util.Arrays.stream(inputs).flatMap(List::stream).toList();
    }

    private static Map<RecipeCapability<?>, List<Object>> inputs(Object... values) {
        Map<RecipeCapability<?>, List<Object>> inputs = new HashMap<>();
        inputs.put(ItemRecipeCapability.CAP, List.of(values));
        return inputs;
    }

    private static final class RecipeTypeFactory {

        private static net.minecraft.world.item.crafting.RecipeType<?> create(Identifier id) {
            return net.minecraft.world.item.crafting.RecipeType.simple(id);
        }
    }
}
