package com.gregtechceu.gtceu.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.client.model.machine.MachineBlockStateModel;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.cover.DynamicCoverRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.cover.IDynamicCoverRenderer;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Target 26.2 deferred BER bridge for machine render descriptors.
 *
 * <p>Live machines are inspected only during extraction. Submission resolves the current baked descriptor list from
 * the active model set and consumes only immutable, renderer-specific snapshots.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MachineBlockEntityRenderer implements BlockEntityRenderer<MetaMachine, MachineRenderSnapshot> {

    public MachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public MachineRenderSnapshot createRenderState() {
        return new MachineRenderSnapshot();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void extractRenderState(MetaMachine machine, MachineRenderSnapshot state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(machine, state, breakProgress);
        var blockState = machine.getBlockState();
        MachineBlockStateModel machineModel = getMachineModel(blockState, machine.getDefinition());
        if (machineModel == null) {
            state.capture(machine.getDefinition(), blockState, List.of(), List.of());
            return;
        }

        List<MachineRenderSnapshot.DynamicRenderEntry> snapshots = new ArrayList<>();
        List<DynamicRender<?, ?>> descriptors = machineModel.dynamicRenders();
        for (int index = 0; index < descriptors.size(); index++) {
            DynamicRender descriptor = descriptors.get(index);
            if (!descriptor.shouldRender(machine, cameraPosition)) continue;
            DynamicRenderSnapshot snapshot = descriptor.extractRenderState(machine, partialTicks);
            if (snapshot != null) {
                snapshots.add(new MachineRenderSnapshot.DynamicRenderEntry(index, snapshot));
            }
        }

        List<DynamicCoverRenderSnapshot> coverSnapshots = new ArrayList<>();
        for (var face : GTUtil.DIRECTIONS) {
            CoverBehavior cover = machine.getCoverContainer().getCoverAtSide(face);
            if (cover == null) continue;
            IDynamicCoverRenderer coverRenderer = cover.getDynamicRenderer().get();
            if (coverRenderer == null) continue;
            DynamicCoverRenderSnapshot snapshot = coverRenderer.extractRenderState(machine, face, partialTicks);
            if (snapshot != null) coverSnapshots.add(snapshot);
        }
        state.capture(machine.getDefinition(), blockState, snapshots, coverSnapshots);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void submit(MachineRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        MachineBlockStateModel machineModel = getMachineModel(state.blockState(), state.definition());
        if (machineModel == null) {
            return;
        }

        List<DynamicRender<?, ?>> descriptors = machineModel.dynamicRenders();
        for (MachineRenderSnapshot.DynamicRenderEntry entry : state.dynamicRenders()) {
            if (entry.descriptorIndex() < 0 || entry.descriptorIndex() >= descriptors.size()) continue;
            descriptors.get(entry.descriptorIndex()).submitRenderState(entry.snapshot(), poseStack, collector, camera);
        }
        for (DynamicCoverRenderSnapshot cover : state.dynamicCovers()) {
            cover.submit(poseStack, collector);
        }
    }

    @Override
    public AABB getRenderBoundingBox(MetaMachine machine) {
        return getMachineRenderBoundingBox(machine);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static AABB getMachineRenderBoundingBox(MetaMachine machine) {
        BlockPos pos = machine.getBlockPos();
        AABB bounds = new AABB(Vec3.atLowerCornerOf(pos.offset(-1, 0, -1)),
                Vec3.atLowerCornerOf(pos.offset(2, 2, 2)));
        MachineBlockStateModel machineModel = getMachineModel(machine.getBlockState(), machine.getDefinition());
        if (machineModel == null) {
            return bounds;
        }
        for (DynamicRender descriptor : machineModel.dynamicRenders()) {
            bounds = bounds.minmax(descriptor.getRenderBoundingBox(machine));
        }
        return bounds;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // 26.2 makes this renderer-level rather than per block entity; per-machine visibility is filtered below.
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean shouldRender(MetaMachine machine, Vec3 cameraPosition) {
        MachineBlockStateModel machineModel = getMachineModel(machine.getBlockState(), machine.getDefinition());
        if (machineModel == null) {
            return false;
        }
        if (machine.getCoverContainer().hasDynamicCovers()) return true;
        for (DynamicRender descriptor : machineModel.dynamicRenders()) {
            if (descriptor.shouldRender(machine, cameraPosition)) return true;
        }
        return false;
    }

    private static @Nullable MachineBlockStateModel getMachineModel(
            BlockState state, MachineDefinition definition) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        MachineBlockStateModel machineModel = MachineBlockStateModel.find(model);
        return machineModel != null && machineModel.getDefinition() == definition ? machineModel : null;
    }
}
