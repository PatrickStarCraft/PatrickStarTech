package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

/** Selects the active fuel match while preserving the previous-fuel fast path. */
final class JetpackFuelSelection {

    private JetpackFuelSelection() {}

    static boolean isFuelFluid(@Nullable FluidStack fluid, Iterable<FluidIngredient> fuels) {
        if (fluid == null || fluid.isEmpty()) return false;
        for (FluidIngredient fuel : fuels) {
            if (fuel.test(fluid)) return true;
        }
        return false;
    }

    static FluidIngredient select(@Nullable FluidStack fluid, @Nullable FluidIngredient previousFuel,
                                  Iterable<FluidIngredient> fuels) {
        if (fluid == null || fluid.isEmpty()) return FluidIngredient.EMPTY;

        if (previousFuel != null && !previousFuel.isEmpty() && previousFuel.test(fluid) &&
                fluid.getAmount() >= previousFuel.getAmount()) {
            return previousFuel;
        }

        for (FluidIngredient fuel : fuels) {
            if (fuel.test(fluid) && fluid.getAmount() >= fuel.getAmount()) {
                return fuel;
            }
        }
        return FluidIngredient.EMPTY;
    }
}
