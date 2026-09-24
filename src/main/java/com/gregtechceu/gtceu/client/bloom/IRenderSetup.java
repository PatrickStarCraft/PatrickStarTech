package com.gregtechceu.gtceu.client.bloom;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Selects the 26.2 geometry pipeline used by a persistent bloom ticket. */
public interface IRenderSetup {

    @OnlyIn(Dist.CLIENT)
    RenderType renderType();
}
