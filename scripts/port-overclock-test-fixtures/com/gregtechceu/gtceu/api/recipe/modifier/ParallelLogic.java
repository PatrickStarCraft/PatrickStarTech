package com.gregtechceu.gtceu.api.recipe.modifier;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

/** Compile-only parallel contract for the unexecuted getModifier path. */
public final class ParallelLogic {
    private ParallelLogic() {}

    public static int getParallelAmount(MetaMachine machine, GTRecipe recipe, int maximum) {
        return 1;
    }
}
