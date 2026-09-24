package com.gregtechceu.gtceu.data.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/** Tag keys needed to compile the real GT fluid-state enum in this isolated test host. */
public final class CustomTags {

    public static final TagKey<Fluid> LIQUID_FLUIDS = fluidTag("liquid_fluids");
    public static final TagKey<Fluid> PLASMA_FLUIDS = fluidTag("plasma_fluids");

    private CustomTags() {}

    private static TagKey<Fluid> fluidTag(String path) {
        return TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("gtceu", path));
    }
}
