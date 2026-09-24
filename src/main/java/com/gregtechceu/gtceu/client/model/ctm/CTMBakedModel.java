package com.gregtechceu.gtceu.client.model.ctm;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Applies connected-texture subdivision while collecting target-version model parts. */
public class CTMBakedModel<T extends BlockStateModel> implements BlockStateModel {

    private final T originalModel;

    public CTMBakedModel(T parent) {
        this.originalModel = parent;
    }

    public T getParent() {
        return this.originalModel;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        this.originalModel.collectParts(random, output);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        List<BlockStateModelPart> parentParts = new ArrayList<>();
        this.originalModel.collectParts(level, pos, state, random, parentParts);
        if (parentParts.isEmpty()) return;

        Map<Direction, TextureConnections> connections = collectConnections(level, pos, state);
        for (BlockStateModelPart part : parentParts) {
            Map<Direction, List<BakedQuad>> connectedQuads = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                connectedQuads.put(direction, CTMMeshBuilder.buildCTMQuads(connections.get(direction),
                        part.getQuads(direction), direction));
            }
            output.add(new ConnectedPart(part, Map.copyOf(connectedQuads)));
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                               RandomSource random) {
        return new GeometryKey(this.originalModel,
                this.originalModel.createGeometryKey(level, pos, state, random), collectConnections(level, pos, state));
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.originalModel.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.originalModel.particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.originalModel.materialFlags();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.originalModel.materialFlags(level, pos, state);
    }

    private static Map<Direction, TextureConnections> collectConnections(BlockAndTintGetter level, BlockPos pos,
                                                                          BlockState state) {
        Map<Direction, TextureConnections> connections = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            TextureConnections faceConnections = TextureConnections.getInstance();
            faceConnections.fillSubmapCache(level, pos, state, direction);
            connections.put(direction, faceConnections);
        }
        return Map.copyOf(connections);
    }

    private record GeometryKey(BlockStateModel parent, @Nullable Object parentKey,
                               Map<Direction, TextureConnections> connections) {}

    private record ConnectedPart(BlockStateModelPart parent,
                                 Map<Direction, List<BakedQuad>> connectedQuads) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? this.parent.getQuads(null) : this.connectedQuads.get(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.parent.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.parent.particleMaterial();
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return this.parent.materialFlags();
        }
    }
}
