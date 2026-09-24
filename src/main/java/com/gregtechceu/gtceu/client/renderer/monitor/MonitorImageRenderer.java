package com.gregtechceu.gtceu.client.renderer.monitor;

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.util.ClientImageCache;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class MonitorImageRenderer implements IMonitorRenderer, IMonitorRenderSnapshotProvider {

    private final String url;

    public MonitorImageRenderer(String url) {
        this.url = url;
    }

    @Override
    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick) {
        if (group.isEmpty()) return null;
        BlockPos rel = group.getRow(0, machine::toRelative).get(0);
        BlockPos size = GTUtil.getLast(group.getRow(-1, machine::toRelative))
                .offset(-rel.getX() + 1, -rel.getY() + 1, -rel.getZ() + 1);
        Identifier textureId = ClientImageCache.getOrLoadTexture(this.url);
        if (textureId == null) return null;
        return new MonitorImageSnapshot(textureId, new BlockPos(rel.getX(), rel.getY(), rel.getZ()),
                size.getX(), size.getY());
    }

    @Override
    public void render(CentralMonitorMachine machine, MonitorGroup group, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (group.isEmpty()) return;
        BlockPos rel = group.getRow(0, machine::toRelative).get(0);
        BlockPos size = GTUtil.getLast(group.getRow(-1, machine::toRelative))
                .offset(-rel.getX() + 1, -rel.getY() + 1, -rel.getZ() + 1);

        poseStack.translate(rel.getX(), rel.getY(), rel.getZ());

        Identifier textureId = ClientImageCache.getOrLoadTexture(url);
        if (textureId == null) return;

        VertexConsumer consumer = buffer.getBuffer(GTRenderTypes.guiTexture(textureId));
        Matrix4f pose = poseStack.last().pose();

        float minX = 0, maxX = size.getX();
        float minY = 0, maxY = size.getY();

        consumer.addVertex(pose, minX, maxY, 0).setColor(0xFFFFFFFF).setUv(0, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, maxX, maxY, 0).setColor(0xFFFFFFFF).setUv(1, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, maxX, minY, 0).setColor(0xFFFFFFFF).setUv(1, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, minX, minY, 0).setColor(0xFFFFFFFF).setUv(0, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    private record MonitorImageSnapshot(Identifier texture, BlockPos origin, int width, int height)
            implements MonitorRenderSnapshot {

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            poseStack.pushPose();
            poseStack.translate(this.origin.getX(), this.origin.getY(), this.origin.getZ());
            collector.submitCustomGeometry(poseStack, GTRenderTypes.guiTexture(this.texture), (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                buffer.addVertex(matrix, 0, this.height, 0).setColor(0xffffffff).setUv(0, 1)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
                buffer.addVertex(matrix, this.width, this.height, 0).setColor(0xffffffff).setUv(1, 1)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
                buffer.addVertex(matrix, this.width, 0, 0).setColor(0xffffffff).setUv(1, 0)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
                buffer.addVertex(matrix, 0, 0, 0).setColor(0xffffffff).setUv(0, 0)
                        .setLight(LightCoordsUtil.FULL_BRIGHT);
            });
            poseStack.popPose();
        }
    }
}
