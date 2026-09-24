package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.client.model.machine.MachineBlockStateModel;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Adds extracted dynamic machine content over the ordinary machine item model. */
public final class MachineItemSpecialRenderer implements SpecialModelRenderer<MachineItemSpecialRenderer.State> {

    @Override
    public @Nullable State extractArgument(ItemStack stack) {
        if (!(stack.getItem() instanceof MetaMachineItem machineItem)) return null;
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .get(machineItem.getBlock().defaultBlockState());
        MachineBlockStateModel machineModel = MachineBlockStateModel.find(model);
        if (machineModel == null || machineModel.getDefinition() != machineItem.getDefinition()) return null;

        BlockState blockState = machineItem.getBlock().defaultBlockState();
        List<BlockStateModelPart> baseParts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, blockState, RandomSource.create(0L), baseParts);
        int[] tintLayers = Minecraft.getInstance().getBlockColors().getTintSources(blockState).stream()
                .mapToInt(source -> source.color(blockState)).toArray();
        boolean translucent = model.hasMaterialFlag(BlockAndTintGetter.EMPTY, BlockPos.ZERO, blockState,
                net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_TRANSLUCENT);

        List<MachineItemRenderSnapshot> snapshots = new ArrayList<>();
        for (DynamicRender<?, ?> dynamicRender : machineModel.getDynamicRenders()) {
            if (!(dynamicRender instanceof MachineItemRenderSnapshotProvider provider)) continue;
            MachineItemRenderSnapshot snapshot = provider.extractItemRenderState(stack);
            if (snapshot != null) snapshots.add(snapshot);
        }
        return new State(baseParts, tintLayers, translucent, snapshots);
    }

    @Override
    public void submit(@Nullable State state, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (state == null) return;
        if (!state.baseParts().isEmpty()) {
            collector.submitMultiLayerBlockModel(poseStack, state.baseParts(), state.translucent(), state.tintLayers(),
                    lightCoords, overlayCoords, outlineColor);
            if (hasFoil) {
                collector.order(1).submitBlockModel(poseStack, RenderTypes.entityGlint(), state.baseParts(),
                        state.tintLayers(), lightCoords, overlayCoords, outlineColor);
            }
        }
        for (MachineItemRenderSnapshot snapshot : state.snapshots()) snapshot.submit(poseStack, collector);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    public record State(List<BlockStateModelPart> baseParts, int[] tintLayers, boolean translucent,
                        List<MachineItemRenderSnapshot> snapshots) {
        public State {
            baseParts = List.copyOf(baseParts);
            tintLayers = tintLayers.clone();
            snapshots = List.copyOf(snapshots);
        }

        @Override
        public int[] tintLayers() {
            return tintLayers.clone();
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<State> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public MachineItemSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new MachineItemSpecialRenderer();
        }
    }
}
