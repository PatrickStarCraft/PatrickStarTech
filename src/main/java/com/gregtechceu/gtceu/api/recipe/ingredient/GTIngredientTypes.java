package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registers custom codecs before recipes are decoded or synchronized. */
public final class GTIngredientTypes {
    private static final DeferredRegister<IngredientType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, GTCEu.MOD_ID);

    static {
        TYPES.register("empty", () -> EmptyIngredient.INGREDIENT_TYPE);
        TYPES.register("sized", () -> SizedIngredient.INGREDIENT_TYPE);
        TYPES.register("circuit", () -> IntCircuitIngredient.INGREDIENT_TYPE);
        TYPES.register("int_provider", () -> IntProviderIngredient.INGREDIENT_TYPE);
        TYPES.register(NBTPredicateIngredient.TYPE.getPath(), () -> NBTPredicateIngredient.INGREDIENT_TYPE);
        TYPES.register(FluidContainerIngredient.TYPE.getPath(), () -> FluidContainerIngredient.INGREDIENT_TYPE);
    }

    private GTIngredientTypes() {}

    public static void init(IEventBus bus) {
        TYPES.register(bus);
    }
}
