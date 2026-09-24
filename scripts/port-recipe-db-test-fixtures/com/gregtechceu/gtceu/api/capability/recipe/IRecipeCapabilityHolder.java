package com.gregtechceu.gtceu.api.capability.recipe;

import java.util.List;
import java.util.Map;

public interface IRecipeCapabilityHolder {

    Map<IO, Map<RecipeCapability<?>, List<IRecipeHandler<?>>>> getCapabilitiesFlat();
}
