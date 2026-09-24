package com.gregtechceu.gtceu.core.mixins.client.bloom;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor
    int getTick();

    @Accessor("resourcePool")
    CrossFrameResourcePool getResourcePool();
}
