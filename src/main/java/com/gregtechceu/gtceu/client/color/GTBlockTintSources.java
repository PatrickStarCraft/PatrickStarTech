package com.gregtechceu.gtceu.client.color;

import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.block.SurfaceRockBlock;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/** Block tint lists adapted from the legacy indexed BlockColor handlers. */
public final class GTBlockTintSources {

    private static final List<BlockTintSource> MATERIAL_LAYERS = indexed(10, (state, level, pos, index) ->
            state.getBlock() instanceof MaterialBlock block ? block.material.getLayerARGB(index) : -1);
    private static final List<BlockTintSource> MATERIAL_PIPE_LAYERS = indexed(2, (state, level, pos, index) -> {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof IPaintable paintable &&
                paintable.isPainted()) {
            return paintable.getPaintingColor();
        }
        return state.getBlock() instanceof MaterialPipeBlock<?, ?, ?> block ?
                block.tinted(state, level, pos, index) : -1;
    });
    private static final List<BlockTintSource> SURFACE_ROCK = indexed(2, (state, level, pos, index) ->
            index == 1 && state.getBlock() instanceof SurfaceRockBlock block ?
                    block.getMaterial().getMaterialRGB() : -1);
    private static final List<BlockTintSource> PAINTABLE_PIPES = indexed(5, (state, level, pos, index) -> {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof PipeBlockEntity<?, ?> pipe) {
            if (pipe.getFrameMaterial() != null) {
                if (index == 3) return pipe.getFrameMaterial().getMaterialRGB();
                if (index == 4) return pipe.getFrameMaterial().getMaterialSecondaryRGB();
            }
            if (pipe.isPainted()) return pipe.getRealColor();
        }
        return -1;
    });
    private static final List<BlockTintSource> MACHINE_LAYERS = indexed(65,
            (state, level, pos, index) -> MetaMachineBlock.colorTinted(state, level, pos, index));

    private GTBlockTintSources() {}

    public static Supplier<List<BlockTintSource>> materialLayers() { return () -> MATERIAL_LAYERS; }
    public static Supplier<List<BlockTintSource>> materialPipeLayers() { return () -> MATERIAL_PIPE_LAYERS; }
    public static Supplier<List<BlockTintSource>> surfaceRock() { return () -> SURFACE_ROCK; }
    public static Supplier<List<BlockTintSource>> paintablePipes() { return () -> PAINTABLE_PIPES; }
    public static Supplier<List<BlockTintSource>> machineLayers() { return () -> MACHINE_LAYERS; }

    private static List<BlockTintSource> indexed(int count, IndexedColor color) {
        return IntStream.range(0, count).<BlockTintSource>mapToObj(index -> new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return color.color(state, null, null, index);
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return color.color(state, level, pos, index);
            }
        }).toList();
    }

    @FunctionalInterface
    private interface IndexedColor {
        int color(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int index);
    }
}
