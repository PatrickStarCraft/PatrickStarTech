package com.gregtechceu.gtceu.client.renderer.placeholder;

import com.gregtechceu.gtceu.api.placeholder.IPlaceholderRenderer;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class QuadPlaceholderRenderer implements IPlaceholderRenderer {

    @Override
    public void render(CentralMonitorMachine machine, MonitorGroup group, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay, CompoundTag tag) {
        poseStack.pushPose();
        VertexConsumer consumer = buffer.getBuffer(GTRenderTypes.getMonitor());
        Matrix4f pose = poseStack.last().pose();

        consumer.addVertex(pose, tag.getFloat("x1").orElse(0.0F), tag.getFloat("y1").orElse(0.0F), 0)
                .setColor(tag.getInt("color1").orElse(0))
                .setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, tag.getFloat("x2").orElse(0.0F), tag.getFloat("y2").orElse(0.0F), 0)
                .setColor(tag.getInt("color2").orElse(0))
                .setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, tag.getFloat("x3").orElse(0.0F), tag.getFloat("y3").orElse(0.0F), 0)
                .setColor(tag.getInt("color3").orElse(0))
                .setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, tag.getFloat("x4").orElse(0.0F), tag.getFloat("y4").orElse(0.0F), 0)
                .setColor(tag.getInt("color4").orElse(0))
                .setLight(LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
    }
}
