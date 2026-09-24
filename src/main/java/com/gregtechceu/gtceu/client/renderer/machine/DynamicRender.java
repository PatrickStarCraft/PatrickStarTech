package com.gregtechceu.gtceu.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderModelOwner;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DynamicRender<T extends IMachineFeature, S extends DynamicRender<T, S>>
                                   implements Comparable<DynamicRender<T, S>> {

    public static final Codec<DynamicRender<?, ?>> CODEC = DynamicRenderManager.TYPE_CODEC
            .dispatchStable(DynamicRender::getType, DynamicRenderType::codec);

    @Getter
    @Setter
    protected MachineRenderModelOwner parent;

    public DynamicRender() {}

    public abstract DynamicRenderType<T, S> getType();

    /** Extracts immutable per-frame input for the 26.2 deferred renderer path. */
    public @Nullable DynamicRenderSnapshot extractRenderState(T machine, float partialTicks) {
        return null;
    }

    /** Submits previously extracted data. Implementations must not query a live machine or world here. */
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack,
                                  SubmitNodeCollector collector, CameraRenderState camera) {}

    public MachineDefinition getDefinition() {
        return parent.getDefinition();
    }

    public boolean shouldRenderOffScreen(T machine) {
        return false;
    }

    public boolean shouldRender(T machine, Vec3 cameraPos) {
        return Vec3.atCenterOf(machine.self().getBlockPos()).closerThan(cameraPos, this.getViewDistance());
    }

    public int getViewDistance() {
        return 64;
    }

    public AABB getRenderBoundingBox(T machine) {
        BlockPos pos = machine.self().getBlockPos();
        return new AABB(Vec3.atLowerCornerOf(pos.offset(-1, 0, -1)),
                Vec3.atLowerCornerOf(pos.offset(2, 2, 2)));
    }

    @Override
    public int compareTo(@NotNull DynamicRender<T, S> o) {
        return this.getType().compareTo(o.getType());
    }

    public boolean isBlockEntityRenderer() {
        return true;
    }
}
