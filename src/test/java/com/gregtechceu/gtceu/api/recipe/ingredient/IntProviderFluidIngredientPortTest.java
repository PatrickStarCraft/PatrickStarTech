package com.gregtechceu.gtceu.api.recipe.ingredient;

import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.component.CustomData;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.fluid.FluidStackMapIngredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntProviderFluidIngredientPortTest {

    @BeforeAll
    static void bindFluidComponents() {
        for (var fluid : java.util.List.of(Fluids.WATER, Fluids.LAVA)) {
            if (!fluid.builtInRegistryHolder().areComponentsBound()) {
                fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
            }
        }
    }

    @Test
    void matchingChecksFluidAndDataButNotCurrentAmount() {
        var ingredient = IntProviderFluidIngredient.of(FluidIngredient.of(Fluids.WATER, 1), UniformInt.of(1, 5));

        assertTrue(ingredient.test(new FluidStack(Fluids.WATER, 3)));
        assertTrue(ingredient.test(new FluidStack(Fluids.WATER, 64)));
        assertFalse(ingredient.test(new FluidStack(Fluids.LAVA, 3)));
    }

    @Test
    void representativeStacksAppearOnlyAfterTheKnownSampleIsCollapsed() {
        var ingredient = IntProviderFluidIngredient.of(FluidIngredient.of(Fluids.WATER, 1), UniformInt.of(1, 5));

        assertEquals(0, ingredient.getStacks().length);
        ingredient.setSampledCount(3);
        var collapsed = ingredient.collapse();

        assertEquals(1, collapsed.getStacks().length);
        assertEquals(3, collapsed.getStacks()[0].getAmount());
        assertEquals(Fluids.WATER, collapsed.getStacks()[0].getFluid());
    }

    @Test
    void unknownFluidIdsFailDuringJsonDecode() {
        var stringValue = new JsonObject();
        stringValue.addProperty("value", "gtceu:missing_fluid_for_port_test");
        stringValue.addProperty("amount", 10);

        var objectValue = new JsonObject();
        var fluid = new JsonObject();
        fluid.addProperty("fluid", "gtceu:missing_fluid_for_port_test");
        objectValue.add("value", fluid);
        objectValue.addProperty("amount", 10);

        assertThrows(JsonParseException.class, () -> FluidIngredient.fromJson(stringValue));
        assertThrows(JsonParseException.class, () -> FluidIngredient.fromJson(objectValue));
    }

    @Test
    void networkRoundTripPreservesProviderTypeSampleAndInnerFluid() {
        var fluidData = new CompoundTag();
        fluidData.putString("grade", "treated");
        var innerFluid = new FluidStack(Fluids.WATER, 125);
        innerFluid.set(DataComponents.CUSTOM_DATA, CustomData.of(fluidData));
        var inner = FluidIngredient.of(innerFluid);
        var provider = BiasedToBottomInt.of(2, 10);
        var ingredient = IntProviderFluidIngredient.of(inner, provider);
        ingredient.setSampledCount(6);

        var decoded = roundTrip(ingredient);

        assertEquals(provider, decoded.getCountProvider());
        assertEquals(6, decoded.getSampledCount());
        assertTrue(decoded.test(innerFluid.copyWithAmount(1)));
        assertFalse(decoded.test(new FluidStack(Fluids.WATER, 1)));
    }

    @Test
    void networkRoundTripPreservesZeroSampledCount() {
        var ingredient = IntProviderFluidIngredient.of(FluidIngredient.of(Fluids.WATER, 1), UniformInt.of(0, 4));
        ingredient.setSampledCount(0);

        assertEquals(0, roundTrip(ingredient).getSampledCount());
    }

    @Test
    void amountInsensitiveFluidKeysAggregateWithConsistentHashes() {
        var oneBucket = FluidIngredient.of(Fluids.WATER, 100);
        var anotherBucket = FluidIngredient.of(Fluids.WATER, 250);
        assertEquals(oneBucket, anotherBucket);
        assertEquals(oneBucket.hashCode(), anotherBucket.hashCode());

        var amountsByIngredient = new Object2LongOpenHashMap<FluidIngredient>();
        amountsByIngredient.addTo(oneBucket, 100);
        amountsByIngredient.addTo(anotherBucket, 250);
        assertEquals(350, amountsByIngredient.getLong(oneBucket));
        assertEquals(350, amountsByIngredient.getLong(anotherBucket));

        var forwardOrder = FluidIngredient.of(java.util.List.of(Fluids.WATER, Fluids.LAVA), 4, null);
        var reverseOrder = FluidIngredient.of(java.util.List.of(Fluids.LAVA, Fluids.WATER), 4, null);
        assertEquals(forwardOrder, reverseOrder);
        assertEquals(forwardOrder.hashCode(), reverseOrder.hashCode());
    }

    @Test
    void fluidLookupKeysHashByTheFluidPredicateNotStackAmount() {
        var oneBucket = FluidStackMapIngredient.from(FluidIngredient.of(Fluids.WATER, 100)).get(0);
        var anotherBucket = FluidStackMapIngredient.from(FluidIngredient.of(Fluids.WATER, 250)).get(0);
        assertEquals(oneBucket, anotherBucket);
        assertEquals(oneBucket.hashCode(), anotherBucket.hashCode());

        var keys = new Object2ObjectOpenHashMap<AbstractMapIngredient, Integer>();
        keys.put(oneBucket, 1);
        keys.put(anotherBucket, 2);
        assertEquals(1, keys.size());
        assertEquals(2, keys.get(oneBucket));

        var directStack = FluidStackMapIngredient.from(new FluidStack(Fluids.WATER, 2)).get(0);
        var anotherDirectStack = FluidStackMapIngredient.from(new FluidStack(Fluids.WATER, 4)).get(0);
        assertTrue(directStack.equals(directStack));
        assertTrue(directStack.equals(anotherDirectStack));
        assertEquals(directStack.hashCode(), anotherDirectStack.hashCode());
        assertTrue(oneBucket.equals(directStack));
        assertTrue(directStack.equals(oneBucket));
        assertEquals(oneBucket.hashCode(), directStack.hashCode());

        var rangedA = IntProviderFluidIngredient.of(FluidIngredient.of(Fluids.WATER, 1), UniformInt.of(1, 5));
        var rangedB = IntProviderFluidIngredient.of(FluidIngredient.of(Fluids.WATER, 1), UniformInt.of(4, 5));
        rangedA.setSampledCount(2);
        rangedB.setSampledCount(4);
        var rangedKeyA = FluidStackMapIngredient.from(rangedA).get(0);
        var rangedKeyB = FluidStackMapIngredient.from(rangedB).get(0);
        assertEquals(rangedKeyA, rangedKeyB);
        assertEquals(rangedKeyA.hashCode(), rangedKeyB.hashCode());
    }

    private static IntProviderFluidIngredient roundTrip(IntProviderFluidIngredient ingredient) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ingredient.toNetwork(buffer);
            var decoded = IntProviderFluidIngredient.fromNetwork(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally {
            buffer.release();
        }
    }
}
