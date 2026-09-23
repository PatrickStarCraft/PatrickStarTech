package com.gregtechceu.gtceu.data.tags;

import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.world.level.material.Fluid;

import com.tterrag.registrate.providers.RegistrateTagsProvider;

public class FluidTagLoader {

    public static void init(RegistrateTagsProvider.Impl<Fluid> provider) {
        provider.tag(CustomTags.LIGHTER_FLUIDS).add(GTMaterials.Butane.getFluid().builtInRegistryHolder().key(),
                GTMaterials.Propane.getFluid().builtInRegistryHolder().key());
        provider.tag(CustomTags.HPCA_COOLANTS).add(GTMaterials.PCBCoolant.getFluid().builtInRegistryHolder().key());
    }
}
