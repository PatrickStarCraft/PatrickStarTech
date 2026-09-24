package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.AssemblyLineMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

public class AssemblyLineRender extends DynamicRender<AssemblyLineMachine, AssemblyLineRender> {

    public static final MapCodec<AssemblyLineRender> CODEC = MapCodec.unit(AssemblyLineRender::new);
    public static final DynamicRenderType<AssemblyLineMachine, AssemblyLineRender> TYPE = new DynamicRenderType<>(
            AssemblyLineRender.CODEC);

    public AssemblyLineRender() {}

    @Override
    public DynamicRenderType<AssemblyLineMachine, AssemblyLineRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(AssemblyLineMachine machine, Vec3 cameraPos) {
        return (machine.recipeLogic.isWorking()) && super.shouldRender(machine, cameraPos);
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(AssemblyLineMachine machine, float partialTicks) {
        GTRecipe recipe = machine.getRecipeLogic().getLastUnrolledRecipe();
        if (recipe == null) return new AssemblyLineSnapshot(List.of());

        int asslineColor = Long.decode(ConfigHolder.INSTANCE.client.renderer.assemblyLineLaser).intValue();
        float progress = machine.getProgress() / (float) machine.getMaxProgress();
        int recipeInputs = Math.max(
                recipe.getInputContents(ItemRecipeCapability.CAP).size(),
                recipe.getInputContents(FluidRecipeCapability.CAP).size());
        progress *= recipeInputs;
        Direction down = RelativeDirection.DOWN.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        Direction back = RelativeDirection.BACK.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());

        List<Line> lines = new ArrayList<>();
        BlockPos.MutableBlockPos pos = BlockPos.ZERO.offset(down.getUnitVec3i()).mutable();
        int opaqueColor = asslineColor | 0xff000000;
        for (int i = 0; i < (int) progress; i++) {
            appendLine(lines, pos, down, opaqueColor);
            pos.move(back.getUnitVec3i().multiply(2));
            appendLine(lines, pos, down, opaqueColor);
            pos.move(back.getOpposite().getUnitVec3i().multiply(2)).move(right.getUnitVec3i());
        }
        int partialColor = asslineColor | ((int) ((progress - (int) progress) * 255.0F) << 24);
        appendLine(lines, pos, down, partialColor);
        pos.move(back.getUnitVec3i().multiply(2));
        appendLine(lines, pos, down, partialColor);
        return new AssemblyLineSnapshot(List.copyOf(lines));
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState camera) {
        if (!(state instanceof AssemblyLineSnapshot snapshot) || snapshot.lines().isEmpty()) return;
        collector.submitCustomGeometry(poseStack, GTRenderTypes.assemblyLine(), (pose, buffer) -> {
            for (Line line : snapshot.lines()) {
                RenderBufferHelper.renderLine(buffer, pose, line.from(), line.to(), 0.03, line.color());
            }
        });
    }

    private static void appendLine(List<Line> lines, BlockPos pos, Direction down, int color) {
        Vec3 top = Vec3.atBottomCenterOf(pos.offset(down.getOpposite().getUnitVec3i()));
        Vec3 bottom = Vec3.atBottomCenterOf(pos);
        lines.add(new Line(bottom, top, color));
    }

    private record Line(Vec3 from, Vec3 to, int color) {}

    private record AssemblyLineSnapshot(List<Line> lines) implements DynamicRenderSnapshot {
        private AssemblyLineSnapshot {
            lines = List.copyOf(lines);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AssemblyLineMachine machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(AssemblyLineMachine machine) {
        Direction down = RelativeDirection.DOWN.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        Direction back = RelativeDirection.BACK.getRelativeFacing(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        Direction right = RelativeDirection.RIGHT.getRelativeFacing(machine.getFrontFacing(),
                machine.getUpwardsFacing(), machine.isFlipped());
        AABB aabb = new AABB(machine.getBlockPos()).expandTowards(Vec3.atLowerCornerOf(down.getUnitVec3i().multiply(2)))
                .expandTowards(Vec3.atLowerCornerOf(down.getOpposite().getUnitVec3i()))
                .expandTowards(Vec3.atLowerCornerOf(back.getUnitVec3i().multiply(2)))
                .expandTowards(Vec3.atLowerCornerOf(right.getUnitVec3i().multiply(17)));
        return aabb;
    }
}
