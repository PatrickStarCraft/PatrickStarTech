package com.gregtechceu.gtceu.client.renderer.monitor;

import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;

/** One monitor module's immutable display data for a frame. */
public interface MonitorRenderSnapshot {

    void submit(PoseStack poseStack, SubmitNodeCollector collector);
}
