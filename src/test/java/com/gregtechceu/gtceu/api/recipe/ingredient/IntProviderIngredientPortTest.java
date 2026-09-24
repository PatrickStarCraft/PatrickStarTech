package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.ItemStackMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.item.DataComponentItemStackMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntProviderIngredientPortTest {

    @BeforeAll
    static void bindItemComponents() {
        for (var item : new net.minecraft.world.item.Item[] {
                Items.BRICK, Items.COBBLESTONE, Items.PAPER
        }) {
            if (!item.builtInRegistryHolder().areComponentsBound()) {
                item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64).build());
            }
        }
        MapIngredientTypeManager.registerMapIngredient(
                DataComponentIngredient.class, DataComponentItemStackMapIngredient::from);
        MapIngredientTypeManager.registerMapIngredient(
                ItemStack.class, DataComponentItemStackMapIngredient::from);
    }

    @Test
    void matchesItemTypeAndPreservesComponentAwareDelegates() {
        var ingredient = IntProviderIngredient.of(new ItemStack(Items.BRICK), UniformInt.of(1, 5));
        assertTrue(ingredient.test(new ItemStack(Items.BRICK, 3)));
        assertTrue(ingredient.test(new ItemStack(Items.BRICK, 64)),
                "count constraints are sampled separately from item matching");
        assertFalse(ingredient.test(new ItemStack(Items.COBBLESTONE, 3)));
        assertFalse(ingredient.isSimple(), "custom count metadata must be retained in synchronization");

        ItemStack namedBrick = new ItemStack(Items.BRICK);
        namedBrick.set(DataComponents.CUSTOM_NAME, Component.literal("named brick"));
        var componentAware = IntProviderIngredient.of(namedBrick, UniformInt.of(1, 5));
        var rangedDelegate = (IntProviderIngredient) componentAware.getCustomIngredient();
        assertInstanceOf(DataComponentIngredient.class, rangedDelegate.getInner().getCustomIngredient());
        assertTrue(componentAware.test(namedBrick.copyWithCount(3)));
        assertFalse(componentAware.test(new ItemStack(Items.BRICK, 3)));

        var sizedComponentAware = SizedIngredient.create(namedBrick);
        assertFalse(sizedComponentAware.isSimple(), "sized custom metadata must be retained in synchronization");
        var sizedDelegate = (SizedIngredient) sizedComponentAware.getCustomIngredient();
        assertInstanceOf(DataComponentIngredient.class, sizedDelegate.getInner().getCustomIngredient());
        assertTrue(SizedIngredient.create(ItemStack.EMPTY).test(ItemStack.EMPTY));
    }

    @Test
    void representativeStacksCopyRollAndCollapseToTheSampledSize() {
        var ingredient = IntProviderIngredient.of(new ItemStack(Items.BRICK), UniformInt.of(1, 5000));
        var ranged = (IntProviderIngredient) ingredient.getCustomIngredient();

        assertEquals(1, ranged.getItems().length);
        assertEquals(5000, ranged.getItems()[0].getCount(), "an unrolled ingredient displays its maximum count");

        int sampled = ranged.rollSampledCount();
        assertTrue(sampled >= 1 && sampled <= 5000);
        var copied = (IntProviderIngredient) ranged.copy().getCustomIngredient();
        assertEquals(sampled, copied.getSampledCount());
        assertEquals(sampled, copied.getItems()[0].getCount());

        var collapsed = ranged.collapse();
        var sized = assertInstanceOf(SizedIngredient.class, collapsed.getCustomIngredient());
        assertEquals(sampled, sized.getAmount());
        assertEquals(1, sized.getItems().length);
        assertTrue(sized.getItems()[0].is(Items.BRICK));
    }

    @Test
    void negativeCountProvidersAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> IntProviderIngredient.of(new ItemStack(Items.BRICK), UniformInt.of(-1, 2)));
    }

    @Test
    void directItemLookupKeysIgnoreCountButKeepComponentsAndIdentity() {
        var oneBrick = new ItemStackMapIngredient(new ItemStack(Items.BRICK, 1));
        var stackOfBricks = new ItemStackMapIngredient(new ItemStack(Items.BRICK, 32));
        var cobblestone = new ItemStackMapIngredient(new ItemStack(Items.COBBLESTONE, 32));

        assertEquals(oneBrick, stackOfBricks);
        assertEquals(oneBrick.hashCode(), stackOfBricks.hashCode());
        assertNotEquals(oneBrick, cobblestone);

        var named = new ItemStack(Items.BRICK, 32);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
        assertNotEquals(oneBrick, new ItemStackMapIngredient(named));
    }

    @Test
    void componentIngredientMapKeysMatchSymmetricallyAndPreservePatchEquality() {
        var expectedData = new CompoundTag();
        expectedData.putString("grade", "treated");
        var ingredientStack = new ItemStack(Items.BRICK);
        ingredientStack.set(DataComponents.CUSTOM_DATA, CustomData.of(expectedData));
        Ingredient partial = DataComponentIngredient.of(false, ingredientStack);
        var lookupKey = new ItemStackMapIngredient(new ItemStack(Items.BRICK), partial);

        var matchingStack = ingredientStack.copy();
        matchingStack.set(DataComponents.CUSTOM_NAME, Component.literal("extra component type"));
        var matchingKey = new ItemStackMapIngredient(matchingStack);
        assertEquals(lookupKey, matchingKey);
        assertEquals(matchingKey, lookupKey);
        assertEquals(lookupKey.hashCode(), matchingKey.hashCode());

        var extraData = expectedData.copy();
        extraData.putBoolean("extra", true);
        var extraDataStack = new ItemStack(Items.BRICK);
        extraDataStack.set(DataComponents.CUSTOM_DATA, CustomData.of(extraData));
        assertNotEquals(lookupKey, new ItemStackMapIngredient(extraDataStack));
    }

    @Test
    void sizedAndRangedIngredientKeysIgnoreTheirCountMetadata() {
        Ingredient ordinary = Ingredient.of(Items.BRICK);
        Ingredient sized = SizedIngredient.create(ordinary, 5);
        Ingredient ranged = IntProviderIngredient.of(new ItemStack(Items.BRICK), UniformInt.of(1, 20));

        var ordinaryKey = new ItemStackMapIngredient(new ItemStack(Items.BRICK), ordinary);
        var sizedKey = new ItemStackMapIngredient(new ItemStack(Items.BRICK, 5), sized);
        var rangedKey = new ItemStackMapIngredient(new ItemStack(Items.BRICK, 20), ranged);

        assertEquals(ordinaryKey, sizedKey);
        assertEquals(sizedKey, rangedKey);
        assertEquals(rangedKey, ordinaryKey);
        assertEquals(ordinaryKey.hashCode(), rangedKey.hashCode());
    }

    @Test
    void mapManagerDispatchesPartialAndStrictComponentIngredientsToMatchingStackKeys() {
        CompoundTag requestedData = new CompoundTag();
        requestedData.putString("grade", "treated");
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_DATA, CustomData.of(requestedData)).build();
        Ingredient partial = DataComponentIngredient.of(false, patch, Items.BRICK);

        List<AbstractMapIngredient> partialKeys =
                MapIngredientTypeManager.getFrom(partial, ItemRecipeCapability.CAP);
        ItemStack partialMatch = new ItemStack(Items.BRICK);
        partialMatch.set(DataComponents.CUSTOM_DATA, CustomData.of(requestedData));
        partialMatch.set(DataComponents.CUSTOM_NAME, Component.literal("additional component type"));
        List<AbstractMapIngredient> partialStackKeys =
                MapIngredientTypeManager.getFrom(partialMatch, ItemRecipeCapability.CAP);

        assertTrue(partialKeys.stream().anyMatch(DataComponentItemStackMapIngredient.class::isInstance),
                "the Ingredient wrapper should dispatch to its DataComponentIngredient delegate");
        assertTrue(partialKeys.stream().anyMatch(partialStackKeys::contains),
                "a non-exhaustive ingredient should share a key with a stack carrying its specified component");

        CompoundTag extraData = requestedData.copy();
        extraData.putBoolean("extra", true);
        ItemStack mismatchedData = new ItemStack(Items.BRICK);
        mismatchedData.set(DataComponents.CUSTOM_DATA, CustomData.of(extraData));
        assertFalse(partialKeys.stream().anyMatch(
                MapIngredientTypeManager.getFrom(mismatchedData, ItemRecipeCapability.CAP)::contains),
                "non-exhaustive matching still requires exact equality inside CUSTOM_DATA");
        assertFalse(partialKeys.stream().anyMatch(
                MapIngredientTypeManager.getFrom(new ItemStack(Items.COBBLESTONE), ItemRecipeCapability.CAP)::contains),
                "component ingredient lookup must preserve item identity");

        ItemStack strictStack = new ItemStack(Items.PAPER);
        strictStack.set(DataComponents.CUSTOM_DATA, CustomData.of(requestedData));
        Ingredient strict = DataComponentIngredient.of(true, strictStack);
        List<AbstractMapIngredient> strictKeys =
                MapIngredientTypeManager.getFrom(strict, ItemRecipeCapability.CAP);
        assertTrue(strictKeys.stream().anyMatch(
                MapIngredientTypeManager.getFrom(strictStack.copy(), ItemRecipeCapability.CAP)::contains),
                "an exhaustive ingredient should match the same component set");

        ItemStack extraComponent = strictStack.copy();
        extraComponent.set(DataComponents.CUSTOM_NAME, Component.literal("not allowed by exhaustive matching"));
        assertFalse(strictKeys.stream().anyMatch(
                MapIngredientTypeManager.getFrom(extraComponent, ItemRecipeCapability.CAP)::contains),
                "an exhaustive ingredient should reject additional component types");
    }
}
