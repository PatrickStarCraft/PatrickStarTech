package com.gregtechceu.gtceu.api.item.data;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentIngredientPortTest {

    @BeforeAll
    static void bindItemComponents() {
        bind(Items.PAPER);
        bind(Items.STICK);
    }

    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64).build());
        }
    }

    private static ItemStack paperWithCustomData(String key, String value) {
        var stack = new ItemStack(Items.PAPER);
        var data = new CompoundTag();
        data.putString(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    @Test
    void nonExhaustiveMatchingAllowsAdditionalComponentTypes() {
        ItemStack expected = paperWithCustomData("grade", "treated");
        var ingredient = DataComponentIngredient.of(false, expected);
        ItemStack candidate = expected.copy();
        candidate.set(DataComponents.CUSTOM_NAME, Component.literal("extra component type"));

        assertTrue(ingredient.test(candidate));
    }

    @Test
    void specifiedCustomDataMustMatchAsAWholeValue() {
        ItemStack expected = paperWithCustomData("grade", "treated");
        var ingredient = DataComponentIngredient.of(false, expected);
        ItemStack candidate = paperWithCustomData("grade", "treated");
        CompoundTag candidateData = candidate.get(DataComponents.CUSTOM_DATA).copyTag();
        candidateData.putBoolean("other_mod", true);
        candidate.set(DataComponents.CUSTOM_DATA, CustomData.of(candidateData));

        assertFalse(ingredient.test(candidate));
    }

    @Test
    void exhaustiveMatchingAcceptsExactComponentsAndRejectsAnExtraType() {
        ItemStack expected = paperWithCustomData("grade", "treated");
        var ingredient = DataComponentIngredient.of(true, expected);

        assertTrue(ingredient.test(expected.copy()));

        ItemStack candidateWithExtra = expected.copy();
        candidateWithExtra.set(DataComponents.CUSTOM_NAME, Component.literal("extra component type"));
        assertFalse(ingredient.test(candidateWithExtra));
    }

    @Test
    void itemIdentityStillRestrictsTheIngredient() {
        var ingredient = DataComponentIngredient.of(false, paperWithCustomData("grade", "treated"));
        ItemStack stick = new ItemStack(Items.STICK);
        var data = new CompoundTag();
        data.putString("grade", "treated");
        stick.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        assertFalse(ingredient.test(stick));
    }
}
