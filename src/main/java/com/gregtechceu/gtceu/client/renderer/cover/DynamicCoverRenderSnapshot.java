package com.gregtechceu.gtceu.client.renderer.cover;

import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;

/** Immutable cover input with the deferred submission behavior for one extracted frame. */
public interface DynamicCoverRenderSnapshot {

    void submit(PoseStack poseStack, SubmitNodeCollector collector);
}
