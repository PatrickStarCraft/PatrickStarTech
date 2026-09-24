package com.gregtechceu.gtceu.client.bloom.particle;

import com.gregtechceu.gtceu.client.bloom.EffectRenderContext;
import com.gregtechceu.gtceu.client.particle.GTParticle;

import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;

public abstract class GTBloomParticle extends GTParticle {

    public GTBloomParticle(double posX, double posY, double posZ) {
        super(posX, posY, posZ);
    }

    /** Submit this particle's emissive geometry into the bloom target for the current frame. */
    public abstract void renderBloomParticle(SubmitNodeCollector collector, PoseStack poseStack,
                                             EffectRenderContext context);
}
