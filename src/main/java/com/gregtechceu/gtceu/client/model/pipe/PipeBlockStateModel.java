package com.gregtechceu.gtceu.client.model.pipe;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.client.model.CoverBlockStateModel;
import com.gregtechceu.gtceu.client.model.GTModelProperties;
import com.gregtechceu.gtceu.client.model.ctm.CTMModelPartSource;
import com.gregtechceu.gtceu.client.model.item.FacadeBlockStateModel;
import com.gregtechceu.gtceu.client.util.quad.transformers.GTQuadTransformers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Connection-dependent pipe geometry selected only from the meshing snapshot.
 */
public final class PipeBlockStateModel implements DynamicBlockStateModel, CTMModelPartSource {

    private final BlockStateModel center;
    private final Map<Direction, BlockStateModel> connections;
    private final Map<Direction, BlockStateModel> restrictors;
    private final FacadeBlockStateModel facadeModel;
    private final CoverBlockStateModel coverModel = new CoverBlockStateModel();

    public PipeBlockStateModel(BlockStateModel center, Map<Direction, BlockStateModel> connections,
                               Map<Direction, BlockStateModel> restrictors, FacadeBlockStateModel facadeModel) {
        this.center = center;
        this.connections = new EnumMap<>(Direction.class);
        this.connections.putAll(connections);
        this.restrictors = new EnumMap<>(Direction.class);
        this.restrictors.putAll(restrictors);
        this.facadeModel = facadeModel;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                             RandomSource random, List<BlockStateModelPart> output) {
        ModelData data = level.getModelData(pos);
        Integer connectionMask = data.get(GTModelProperties.PIPE_CONNECTION_MASK);
        Integer blockedMask = data.get(GTModelProperties.PIPE_BLOCKED_MASK);
        Integer paintingColor = data.get(GTModelProperties.PIPE_PAINTING_COLOR);
        BlockState frameState = data.get(GTModelProperties.PIPE_FRAME_STATE);
        Map<Direction, ModelData> coverData = data.get(GTModelProperties.COVER_MODEL_DATA);
        Set<Direction> coveredFaces = coverData == null ? Set.of() : Set.copyOf(coverData.keySet());

        long seed = random.nextLong();
        if (connectionMask == null) {
            add(this.center, level, pos, state, RandomSource.create(seed), paintingColor, output);
            this.facadeModel.collectParts(level, pos, state, RandomSource.create(seed), output);
            this.coverModel.collectParts(level, pos, output);
            return;
        } else if (connectionMask != Node.ALL_OPENED) {
            add(this.center, level, pos, state, RandomSource.create(seed), paintingColor, output);
        }
        if (connectionMask != null) {
            for (Direction direction : Direction.values()) {
                if (!PipeBlockEntity.isConnected(connectionMask, direction)) continue;
                add(this.connections.get(direction), level, pos, state, RandomSource.create(seed), paintingColor, output);
                if (blockedMask != null && PipeBlockEntity.isFaceBlocked(blockedMask, direction)) {
                    add(this.restrictors.get(direction), level, pos, state, RandomSource.create(seed), paintingColor,
                            output);
                }
            }
        }
        addFrame(frameState, coveredFaces, level, pos, RandomSource.create(seed), output);
        this.facadeModel.collectParts(level, pos, state, RandomSource.create(seed), output);
        this.coverModel.collectParts(level, pos, output);
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        ModelData data = level.getModelData(pos);
        Integer connections = data.get(GTModelProperties.PIPE_CONNECTION_MASK);
        Integer blocked = data.get(GTModelProperties.PIPE_BLOCKED_MASK);
        BlockState frameState = data.get(GTModelProperties.PIPE_FRAME_STATE);
        Integer paintingColor = data.get(GTModelProperties.PIPE_PAINTING_COLOR);
        Map<Direction, ModelData> coverData = data.get(GTModelProperties.COVER_MODEL_DATA);
        Set<Direction> coveredFaces = coverData == null ? Set.of() : Set.copyOf(coverData.keySet());
        long seed = random.nextLong();
        List<PartKey> keys = new ArrayList<>();
        Object facadeKey = this.facadeModel.createGeometryKey(level, pos, state, RandomSource.create(seed));
        Object coverKey = this.coverModel.geometryKey(level, pos);

        if (connections == null) {
            keys.add(new PartKey(null, key(this.center, level, pos, state, seed)));
            return new GeometryKey(connections, blocked, null, paintingColor, Set.of(), null, List.copyOf(keys),
                    facadeKey, coverKey);
        } else if (connections != Node.ALL_OPENED) {
            keys.add(new PartKey(null, key(this.center, level, pos, state, seed)));
        }
        if (connections != null) {
            for (Direction direction : Direction.values()) {
                if (!PipeBlockEntity.isConnected(connections, direction)) continue;
                keys.add(new PartKey(direction, key(this.connections.get(direction), level, pos, state, seed)));
                if (blocked != null && PipeBlockEntity.isFaceBlocked(blocked, direction)) {
                    keys.add(new PartKey(direction, key(this.restrictors.get(direction), level, pos, state, seed)));
                }
            }
        }
        Object frameKey = frameKey(frameState, level, pos, seed);
        return new GeometryKey(connections, blocked, frameState, paintingColor, coveredFaces, frameKey,
                List.copyOf(keys), facadeKey, coverKey);
    }

    @Override
    public void collectConnectedTextureCandidates(RandomSource random, List<BlockStateModelPart> output) {
        long seed = random.nextLong();
        Set<BlockStateModel> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (this.center != null && seen.add(this.center)) collectCandidates(this.center, seed, output);
        for (BlockStateModel model : this.connections.values()) {
            if (model != null && seen.add(model)) collectCandidates(model, seed, output);
        }
        for (BlockStateModel model : this.restrictors.values()) {
            if (seen.add(model)) collectCandidates(model, seed, output);
        }
    }

    private static void collectCandidates(BlockStateModel model, long seed, List<BlockStateModelPart> output) {
        RandomSource random = RandomSource.create(seed);
        if (model instanceof CTMModelPartSource source) {
            source.collectConnectedTextureCandidates(random, output);
        } else {
            model.collectParts(random, output);
        }
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (this.center != null) return this.center.particleMaterial(level, pos, state);
        for (BlockStateModel model : this.connections.values()) {
            if (model != null) return model.particleMaterial(level, pos, state);
        }
        BlockState frameState = level.getModelData(pos).get(GTModelProperties.PIPE_FRAME_STATE);
        if (frameState != null) return frameModel(frameState).particleMaterial(level, pos, frameState);
        throw new IllegalStateException("Pipe model has no center, connection, or frame model");
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int flags = this.center == null ? 0 : this.center.materialFlags(level, pos, state);
        for (BlockStateModel model : this.connections.values()) flags |= model.materialFlags(level, pos, state);
        for (BlockStateModel model : this.restrictors.values()) flags |= model.materialFlags(level, pos, state);
        BlockState frameState = level.getModelData(pos).get(GTModelProperties.PIPE_FRAME_STATE);
        if (frameState != null) flags |= frameModel(frameState).materialFlags(level, pos, frameState);
        flags |= this.facadeModel.materialFlags(level, pos, state);
        flags |= this.coverModel.materialFlags(level, pos);
        return flags;
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        if (this.center != null) return this.center.particleMaterial();
        return this.connections.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Pipe model has no center or connection model"))
                .particleMaterial();
    }

    @Override
    @Deprecated
    public int materialFlags() {
        int flags = this.center == null ? 0 : this.center.materialFlags();
        for (BlockStateModel model : this.connections.values()) flags |= model.materialFlags();
        for (BlockStateModel model : this.restrictors.values()) flags |= model.materialFlags();
        return flags;
    }

    private static void add(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state,
                            RandomSource random, @Nullable Integer paintingColor,
                            List<BlockStateModelPart> output) {
        if (model == null) return;
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, pos, state, random, parts);
        if (paintingColor == null || paintingColor == -1) {
            output.addAll(parts);
        } else {
            for (BlockStateModelPart part : parts) output.add(new PaintedPart(part, paintingColor));
        }
    }

    private static Object key(BlockStateModel model, BlockAndTintGetter level, BlockPos pos,
                              BlockState state, long seed) {
        return model == null ? null : model.createGeometryKey(level, pos, state, RandomSource.create(seed));
    }

    private static void addFrame(@Nullable BlockState frameState, Set<Direction> coveredFaces,
                                 BlockAndTintGetter level, BlockPos pos, RandomSource random,
                                 List<BlockStateModelPart> output) {
        if (frameState == null) return;
        List<BlockStateModelPart> parts = new ArrayList<>();
        frameModel(frameState).collectParts(level, pos, frameState, random, parts);
        for (BlockStateModelPart part : parts) output.add(new FramePart(part, coveredFaces));
    }

    private static BlockStateModel frameModel(BlockState frameState) {
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        return modelSet.get(frameState);
    }

    private static @Nullable Object frameKey(@Nullable BlockState frameState, BlockAndTintGetter level,
                                              BlockPos pos, long seed) {
        if (frameState == null) return null;
        return frameModel(frameState).createGeometryKey(level, pos, frameState, RandomSource.create(seed));
    }

    private record PartKey(Direction direction, Object geometryKey) {}

    private record GeometryKey(Integer connections, Integer blocked, @Nullable BlockState frameState,
                               @Nullable Integer paintingColor, Set<Direction> coveredFaces,
                               @Nullable Object frameKey, List<PartKey> parts, Object facadeKey, Object coverKey) {}

    private record FramePart(BlockStateModelPart original, Set<Direction> coveredFaces)
            implements BlockStateModelPart {

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction != null && this.coveredFaces.contains(direction) ? List.of() :
                    this.original.getQuads(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.original.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.original.materialFlags();
        }
    }

    private record PaintedPart(BlockStateModelPart original, int color) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            List<BakedQuad> quads = this.original.getQuads(direction);
            List<BakedQuad> tinted = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                int tintIndex = quad.materialInfo().tintIndex();
                tinted.add(tintIndex == 0 || tintIndex == 1 ?
                        GTQuadTransformers.setColor(quad, this.color, true) : quad);
            }
            return tinted;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.original.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return this.original.materialFlags();
        }
    }
}
