package com.gregtechceu.gtceu.client.renderer.monitor;

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

/** Immutable four-vertex monitor graphic captured during extraction. */
public record MonitorQuadSnapshot(float[] positions, int[] colors) implements MonitorRenderSnapshot {

    public MonitorQuadSnapshot {
        if (positions.length != 8 || colors.length != 4) {
            throw new IllegalArgumentException("A monitor quad requires four XY vertices and four colors");
        }
        positions = positions.clone();
        colors = colors.clone();
    }

    @Override
    public float[] positions() {
        return this.positions.clone();
    }

    @Override
    public int[] colors() {
        return this.colors.clone();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, GTRenderTypes.getMonitor(), (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(matrix, this.positions[i * 2], this.positions[i * 2 + 1], 0)
                        .setColor(this.colors[i]).setLight(LightCoordsUtil.FULL_BRIGHT);
            }
        });
    }
}
