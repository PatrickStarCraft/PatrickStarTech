package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import java.util.List;

public class CentralMonitorRender extends DynamicRender<CentralMonitorMachine, CentralMonitorRender> {

    // spotless:off
    public static final MapCodec<CentralMonitorRender> CODEC = MapCodec.unit(CentralMonitorRender::new);
    public static final DynamicRenderType<CentralMonitorMachine, CentralMonitorRender> TYPE = new DynamicRenderType<>(CODEC);
    // spotless:on
    private static final float SCREEN_OFFSET_Z = 0.01f;

    public CentralMonitorRender() {}

    @Override
    public DynamicRenderType<CentralMonitorMachine, CentralMonitorRender> getType() {
        return TYPE;
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(CentralMonitorMachine machine, float partialTicks) {
        if (!machine.getRecipeLogic().isWorking()) return null;
        List<MonitorRenderSnapshot> renderers = new java.util.ArrayList<>();
        for (MonitorGroup group : machine.getMonitorGroups()) {
            ItemStack stack = group.getItemStackHandler().getStackInSlot(0);
            if (!(stack.getItem() instanceof ComponentItem item)) continue;
            for (IItemComponent component : item.getComponents()) {
                if (!(component instanceof IMonitorModuleItem module)) continue;
                IMonitorRenderer renderer = module.getRenderer(stack, machine, group);
                MonitorRenderSnapshot snapshot = renderer.extractRenderState(machine, group, partialTicks);
                if (snapshot != null) renderers.add(snapshot);
            }
        }
        if (renderers.isEmpty()) return null;
        return new CentralMonitorSnapshot(machine.getFrontFacing(), machine.getRightDist(), machine.getUpDist(),
                List.copyOf(renderers));
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                  CameraRenderState camera) {
        if (!(state instanceof CentralMonitorSnapshot snapshot)) return;
        poseStack.pushPose();
        RenderUtil.moveToFace(poseStack, 0.5f, 0.5f, 0.5f, snapshot.frontFacing());
        RenderUtil.rotateToFace(poseStack, snapshot.frontFacing(), Direction.NORTH);
        poseStack.translate(-snapshot.rightDist() - 0.5f, -snapshot.upDist() - 0.5f, SCREEN_OFFSET_Z);
        for (MonitorRenderSnapshot renderer : snapshot.renderers()) {
            renderer.submit(poseStack, collector);
        }
        poseStack.popPose();
    }

    private record CentralMonitorSnapshot(Direction frontFacing, int rightDist, int upDist,
                                          List<MonitorRenderSnapshot> renderers) implements DynamicRenderSnapshot {
        private CentralMonitorSnapshot {
            renderers = List.copyOf(renderers);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CentralMonitorMachine machine) {
        return true;
    }

    @Override
    public boolean shouldRender(CentralMonitorMachine machine, Vec3 cameraPos) {
        return machine.isFormed();
    }

    @Override
    public AABB getRenderBoundingBox(CentralMonitorMachine machine) {
        BlockPos pos = machine.getBlockPos();
        BoundingBox bounds = new BoundingBox(
                pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        for (int row = 0; row <= machine.getUpDist() + machine.getDownDist(); row++) {
            for (int col = 0; col <= machine.getLeftDist() + machine.getRightDist(); col++) {
                IMonitorComponent component = machine.getComponent(row, col);
                if (component != null && component.isMonitor()) {
                    // noinspection deprecation
                    bounds.encapsulate(component.getComponentPos());
                }
            }
        }
        return AABB.of(bounds);
    }
}
