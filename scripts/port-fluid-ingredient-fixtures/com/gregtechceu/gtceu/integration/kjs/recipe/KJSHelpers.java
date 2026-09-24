package com.gregtechceu.gtceu.integration.kjs.recipe;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;

/** Optional integration shim: this isolated suite intentionally does not exercise KubeJS tag lookup. */
public final class KJSHelpers {
    private KJSHelpers() {}

    public static Collection<Fluid> getFluidsDuringLoad(TagKey<Fluid> tag) {
        return null;
    }
}
