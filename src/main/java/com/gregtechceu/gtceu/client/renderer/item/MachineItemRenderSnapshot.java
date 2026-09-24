package com.gregtechceu.gtceu.client.renderer.item;

import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;

/** Immutable dynamic machine-item content captured for one item-model update. */
public interface MachineItemRenderSnapshot {

    void submit(PoseStack poseStack, SubmitNodeCollector collector);
}
