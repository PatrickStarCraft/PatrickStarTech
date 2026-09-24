package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.client.bloom.BloomShaderManager;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import static net.minecraft.util.ARGB.*;

public class FusionRingRender extends DynamicRender<FusionReactorMachine, FusionRingRender> {

    // spotless:off
    public static final MapCodec<FusionRingRender> CODEC = MapCodec.unit(FusionRingRender::new);
    public static final DynamicRenderType<FusionReactorMachine, FusionRingRender> TYPE = new DynamicRenderType<>(FusionRingRender.CODEC);
    // spotless:on

    public static final float FADEOUT = 60;

    public FusionRingRender() {}

    @Override
    public DynamicRenderType<FusionReactorMachine, FusionRingRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(FusionReactorMachine machine, Vec3 cameraPos) {
        return (machine.recipeLogic.isWorking() || machine.delta > 0) && super.shouldRender(machine, cameraPos);
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(FusionReactorMachine machine, float partialTicks) {
        boolean working = machine.recipeLogic.isWorking();
        if (!working && machine.delta <= 0) return null;

        float alpha = 1f;
        if (working) {
            machine.lastColor = machine.getColor();
            machine.delta = FADEOUT;
        } else {
            alpha = machine.delta / FADEOUT;
            machine.lastColor = color(Mth.floor(alpha * 255), red(machine.lastColor), green(machine.lastColor),
                    blue(machine.lastColor));
            machine.delta -= Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
        }

        float lerpFactor = Math.abs((Math.abs(machine.getOffsetTimer() % 50) + partialTicks) - 25) / 25;
        float red = Mth.lerp(lerpFactor, red(machine.lastColor), 255) / 255f;
        float green = Mth.lerp(lerpFactor, green(machine.lastColor), 255) / 255f;
        float blue = Mth.lerp(lerpFactor, blue(machine.lastColor), 255) / 255f;
        var back = RelativeDirection.BACK.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        var axis = RelativeDirection.UP.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped()).getAxis();
        float bloomAlpha = working ? 1f : ((machine.lastColor >>> 24) & 0xff) / 255f;
        return new FusionRingSnapshot(back.getStepX() * 7 + 0.5F, back.getStepY() * 7 + 0.5F,
                back.getStepZ() * 7 + 0.5F, red, green, blue, alpha, bloomAlpha, axis,
                BloomShaderManager.isBloomActive());
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                  CameraRenderState camera) {
        if (!(state instanceof FusionRingSnapshot snapshot)) return;
        submitRing(snapshot, poseStack, collector, GTRenderTypes.lightRing(), snapshot.alpha());
        if (snapshot.bloomActive()) {
            submitRing(snapshot, poseStack, collector, GTRenderTypes.bloomLightRing(), snapshot.bloomAlpha());
        }
    }

    private static void submitRing(FusionRingSnapshot snapshot, PoseStack poseStack, SubmitNodeCollector collector,
                                  net.minecraft.client.renderer.rendertype.RenderType renderType, float alpha) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                RenderBufferHelper.renderRing(pose, buffer, snapshot.x(), snapshot.y(), snapshot.z(), 6, 0.2F,
                        10, 20, snapshot.red(), snapshot.green(), snapshot.blue(), alpha, snapshot.axis()));
    }

    private record FusionRingSnapshot(float x, float y, float z, float red, float green, float blue, float alpha,
                                      float bloomAlpha, Direction.Axis axis, boolean bloomActive)
            implements DynamicRenderSnapshot {}

    @Override
    public boolean shouldRenderOffScreen(FusionReactorMachine machine) {
        return machine.recipeLogic.isWorking() || machine.delta > 0;
    }

    @Override
    public AABB getRenderBoundingBox(FusionReactorMachine machine) {
        return new AABB(machine.getBlockPos()).inflate(getViewDistance() / 2.0D);
    }

}
