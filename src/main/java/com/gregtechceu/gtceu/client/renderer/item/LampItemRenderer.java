package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.common.item.LampBlockItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
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

/** Special item renderer for the stack-selected lamp block-state model. */
public final class LampItemRenderer implements SpecialModelRenderer<BlockState> {

    @Override
    public @Nullable BlockState extractArgument(ItemStack stack) {
        if (!(stack.getItem() instanceof LampBlockItem item)) return null;
        return item.getStateFromStack(stack, item.getBlock().defaultBlockState());
    }

    @Override
    public void submit(@Nullable BlockState state, PoseStack poseStack, SubmitNodeCollector collector,
                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (state == null) return;

        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(0L), parts);
        if (parts.isEmpty()) return;

        int[] tintLayers = Minecraft.getInstance().getBlockColors().getTintSources(state).stream()
                .mapToInt(source -> source.color(state)).toArray();
        boolean translucent = model.hasMaterialFlag(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state,
                net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_TRANSLUCENT);
        collector.submitMultiLayerBlockModel(poseStack, parts, translucent, tintLayers,
                lightCoords, overlayCoords, outlineColor);

        if (hasFoil) {
            collector.order(1).submitBlockModel(poseStack, RenderTypes.entityGlint(), parts, tintLayers,
                    lightCoords, overlayCoords, outlineColor);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y, z));
            }
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<BlockState> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public LampItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new LampItemRenderer();
        }
    }
}
