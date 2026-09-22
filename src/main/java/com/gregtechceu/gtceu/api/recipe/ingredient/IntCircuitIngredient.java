package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.stream.Stream;

public final class IntCircuitIngredient implements ICustomIngredient {

    public static final Identifier TYPE = GTCEu.id("circuit");
    public static final int CIRCUIT_MIN = 0;
    public static final int CIRCUIT_MAX = 32;
    public static final MapCodec<IntCircuitIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            net.minecraft.util.ExtraCodecs.intRange(CIRCUIT_MIN, CIRCUIT_MAX).fieldOf("configuration")
                    .forGetter(ingredient -> ingredient.configuration))
            .apply(instance, IntCircuitIngredient::new));
    public static final IngredientType<IntCircuitIngredient> INGREDIENT_TYPE = new IngredientType<>(CODEC);

    private static final IntCircuitIngredient[] INGREDIENTS = new IntCircuitIngredient[CIRCUIT_MAX + 1];

    public static net.minecraft.world.item.crafting.Ingredient of(int configuration) {
        if (configuration < CIRCUIT_MIN || configuration > CIRCUIT_MAX) {
            throw new IndexOutOfBoundsException("Circuit configuration " + configuration + " is out of range");
        }
        IntCircuitIngredient ingredient = INGREDIENTS[configuration];
        if (ingredient == null) {
            INGREDIENTS[configuration] = ingredient = new IntCircuitIngredient(configuration);
        }
        return ingredient.toVanilla();
    }

    private final int configuration;

    private IntCircuitIngredient(int configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GTItems.PROGRAMMED_CIRCUIT.get()) &&
                IntCircuitBehaviour.getCircuitConfiguration(stack) == this.configuration;
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(GTItems.PROGRAMMED_CIRCUIT.get().builtInRegistryHolder());
    }

    public ItemStack[] getItems() {
        return new ItemStack[] { IntCircuitBehaviour.stack(configuration) };
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<IntCircuitIngredient> getType() {
        return INGREDIENT_TYPE;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof IntCircuitIngredient other && configuration == other.configuration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration);
    }
}
