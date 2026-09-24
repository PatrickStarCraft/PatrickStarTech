package com.gregtechceu.gtceu.client.model.item;

import com.gregtechceu.gtceu.client.model.GTModelProperties;
import com.gregtechceu.gtceu.client.model.quad.MeshBuilder;
import com.gregtechceu.gtceu.client.model.quad.MutableQuadView;
import com.gregtechceu.gtceu.client.model.quad.StaticFaceBakery;
import com.gregtechceu.gtceu.client.util.FakeBlockTintGetter;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadPositionForcer;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadReInterpolator;
import com.gregtechceu.gtceu.client.util.quad.transformers.QuadTinter;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Target block-state model for facade cover geometry. It consumes the immutable
 * facade and child ModelData snapshots supplied by the host block entity and
 * never queries that block entity while collecting chunk geometry.
 */
public final class FacadeBlockStateModel implements DynamicBlockStateModel {

    private static final double FACADE_PLANE_BACK = 1.0 / 16;
    private static final Map<Direction, QuadPositionForcer> CLAMPERS = createClampers();
    private final TextureAtlasSprite backPlateSprite;

    public FacadeBlockStateModel(TextureAtlasSprite backPlateSprite) {
        this.backPlateSprite = backPlateSprite;
    }

    @Override
    @Deprecated
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        // Facade state is contextual block-entity data and is unavailable to this legacy overload.
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        for (PreparedFacade prepared : this.prepare(level, pos, random)) {
            output.add(prepared.part());
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        List<Object> keys = new ArrayList<>();
        for (PreparedFacade prepared : this.prepare(level, pos, random)) {
            keys.add(prepared.key());
        }
        return List.copyOf(keys);
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel().particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        FacadeRenderState snapshot = getSnapshot(level, pos);
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        for (Direction direction : Direction.values()) {
            FacadeRenderState.Facade facade = snapshot.facades().get(direction);
            if (facade != null && facade.state().getRenderShape() == RenderShape.MODEL) {
                return models.get(facade.state()).particleMaterial(level, pos, facade.state());
            }
        }
        return models.missingModel().particleMaterial();
    }

    @Override
    @Deprecated
    public int materialFlags() {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel().materialFlags();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int flags = 0;
        FacadeRenderState snapshot = getSnapshot(level, pos);
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        for (Direction direction : Direction.values()) {
            FacadeRenderState.Facade facade = snapshot.facades().get(direction);
            if (facade == null) continue;
            BackPlateGeometry backPlate = this.createBackPlate(direction, facade, snapshot);
            if (backPlate != null) {
                flags |= new BackPlatePart(backPlate.back(), backPlate.culled(), this.backPlateSprite).materialFlags();
            }
            if (facade.state().getRenderShape() == RenderShape.MODEL)
                flags |= models.get(facade.state()).materialFlags(level, pos, facade.state());
        }
        return flags;
    }

    private List<PreparedFacade> prepare(BlockAndTintGetter level, BlockPos pos, RandomSource random) {
        FacadeRenderState snapshot = getSnapshot(level, pos);
        if (snapshot.facades().isEmpty()) return List.of();

        ModelData parentData = level.getModelData(pos);
        Map<Direction, ModelData> childDataBySide = parentData.get(GTModelProperties.COVER_MODEL_DATA);
        BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        List<PreparedFacade> result = new ArrayList<>();

        for (Direction side : Direction.values()) {
            FacadeRenderState.Facade facade = snapshot.facades().get(side);
            if (facade == null) continue;

            BackPlateGeometry backPlate = this.createBackPlate(side, facade, snapshot);
            if (backPlate != null) {
                result.add(new PreparedFacade(new BackPlatePart(backPlate.back(), backPlate.culled(), this.backPlateSprite),
                        backPlate.key()));
            }
            if (facade.state().getRenderShape() != RenderShape.MODEL) continue;

            ModelData childData = childDataBySide == null ? ModelData.EMPTY : childDataBySide.get(side);
            if (childData == null) childData = ModelData.EMPTY;
            BlockState facadeState = facade.state();
            BlockAndTintGetter facadeLevel = new FacadeTintGetter(level, pos, facadeState, childData);
            BlockStateModel facadeModel = modelSet.get(facadeState);
            long seed = random.nextLong();
            Object modelKey = facadeModel.createGeometryKey(facadeLevel, pos, facadeState, RandomSource.create(seed));
            List<BlockStateModelPart> childParts = new ArrayList<>();
            facadeModel.collectParts(facadeLevel, pos, facadeState, RandomSource.create(seed), childParts);

            for (BlockStateModelPart childPart : childParts) {
                IdentityHashMap<Direction, List<BakedQuad>> grouped = new IdentityHashMap<>();
                grouped.put(null, transformQuads(childPart.getQuads(null), null, side, facade,
                        facadeLevel, pos));
                for (Direction cullFace : Direction.values()) {
                    grouped.put(cullFace, transformQuads(childPart.getQuads(cullFace), cullFace, side, facade,
                            facadeLevel, pos));
                }

                List<BakedQuad> unculled = new ArrayList<>(grouped.get(null));
                for (Direction cullFace : Direction.values()) unculled.addAll(grouped.get(cullFace));
                EnumMap<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
                for (Direction cullFace : Direction.values()) {
                    List<BakedQuad> selected = new ArrayList<>(grouped.get(null));
                    if (cullFace == side) selected.addAll(grouped.get(cullFace));
                    culled.put(cullFace, List.copyOf(selected));
                }

                ClippedPart clippedPart = new ClippedPart(childPart, List.copyOf(unculled), Map.copyOf(culled), side,
                        facade.shouldRenderBackSide());
                if (!clippedPart.unculled().isEmpty() || culled.values().stream().anyMatch(quads -> !quads.isEmpty())) {
                    FacadeGeometryKey key = new FacadeGeometryKey(side, facadeState, facade.shouldRenderPlate(),
                            facade.shouldRenderBackSide(), modelKey, clippedPart.unculled(), clippedPart.culled());
                    result.add(new PreparedFacade(clippedPart, key));
                }
            }
        }
        return List.copyOf(result);
    }

    private BackPlateGeometry createBackPlate(Direction attachedSide, FacadeRenderState.Facade facade,
                                              FacadeRenderState snapshot) {
        double thickness = snapshot.coverPlateThickness();
        if (!facade.shouldRenderPlate() || thickness <= 0.0) return null;

        AABB bounds = switch (attachedSide) {
            case DOWN -> StaticFaceBakery.COVER_OVERLAY.setMaxY(thickness);
            case UP -> StaticFaceBakery.COVER_OVERLAY.setMinY(1.0 - thickness);
            case NORTH -> StaticFaceBakery.COVER_OVERLAY.setMaxZ(thickness);
            case SOUTH -> StaticFaceBakery.COVER_OVERLAY.setMinZ(1.0 - thickness);
            case WEST -> StaticFaceBakery.COVER_OVERLAY.setMaxX(thickness);
            case EAST -> StaticFaceBakery.COVER_OVERLAY.setMinX(1.0 - thickness);
        };

        List<BakedQuad> back = List.of(StaticFaceBakery.bakeFace(
                bounds, attachedSide.getOpposite(), this.backPlateSprite, true));
        EnumMap<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        for (Direction cullFace : Direction.values()) {
            // FacadeCoverRenderer.shouldRenderBackPlateForSide excludes the attached face itself.
            if (cullFace == attachedSide || cullFace == attachedSide.getOpposite() ||
                    snapshot.occupiedFaces().contains(cullFace)) continue;
            culled.put(cullFace, List.of(StaticFaceBakery.bakeFace(bounds, cullFace, this.backPlateSprite, true)));
        }
        Map<Direction, List<BakedQuad>> immutableCulled = Map.copyOf(culled);
        BackPlateKey key = new BackPlateKey(attachedSide, thickness, snapshot.occupiedFaces(), back, immutableCulled);
        return new BackPlateGeometry(back, immutableCulled, key);
    }

    private static List<BakedQuad> transformQuads(List<BakedQuad> input, @Nullable Direction cullFace,
                                                   Direction side, FacadeRenderState.Facade facade,
                                                   BlockAndTintGetter level, BlockPos pos) {
        if (input.isEmpty()) return List.of();
        List<BakedQuad> output = new ArrayList<>(input.size());
        QuadPositionForcer clamper = CLAMPERS.get(side);
        for (BakedQuad quad : input) {
            if (quad.direction() != side && (facade.shouldRenderPlate() || !facade.shouldRenderBackSide())) continue;

            MutableQuadView emitter = MeshBuilder.getInstance().getEmitter();
            emitter.fromVanilla(quad, cullFace);
            QuadReInterpolator interpolator = new QuadReInterpolator();
            interpolator.setInputQuad(emitter);

            int tintIndex = emitter.tintIndex();
            if (tintIndex != -1) {
                BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(facade.state(), tintIndex);
                int color = source == null ? -1 : source.colorInWorld(facade.state(), level, pos);
                new QuadTinter(color).transform(emitter);
            }

            clamper.transform(emitter);
            interpolator.transform(emitter);
            output.add(emitter.toBlockBakedQuad());
            emitter.emit();
        }
        return List.copyOf(output);
    }

    private static FacadeRenderState getSnapshot(BlockAndTintGetter level, BlockPos pos) {
        FacadeRenderState snapshot = level.getModelData(pos).get(GTModelProperties.FACADE_RENDER_STATE);
        return snapshot == null ? FacadeRenderState.EMPTY : snapshot;
    }

    private static Map<Direction, QuadPositionForcer> createClampers() {
        EnumMap<Direction, QuadPositionForcer> clampers = new EnumMap<>(Direction.class);
        for (Direction direction : GTUtil.DIRECTIONS) {
            AABB bounds = switch (direction) {
                case DOWN -> StaticFaceBakery.COVER_OVERLAY.setMaxY(FACADE_PLANE_BACK);
                case UP -> StaticFaceBakery.COVER_OVERLAY.setMinY(1.0 - FACADE_PLANE_BACK);
                case NORTH -> StaticFaceBakery.COVER_OVERLAY.setMaxZ(FACADE_PLANE_BACK);
                case SOUTH -> StaticFaceBakery.COVER_OVERLAY.setMinZ(1.0 - FACADE_PLANE_BACK);
                case WEST -> StaticFaceBakery.COVER_OVERLAY.setMaxX(FACADE_PLANE_BACK);
                case EAST -> StaticFaceBakery.COVER_OVERLAY.setMinX(1.0 - FACADE_PLANE_BACK);
            };
            clampers.put(direction, new QuadPositionForcer(bounds));
        }
        return Map.copyOf(clampers);
    }

    private static final class FacadeTintGetter extends FakeBlockTintGetter {
        private final BlockPos facadePos;
        private final ModelData childData;

        private FacadeTintGetter(BlockAndTintGetter parent, BlockPos pos, BlockState facadeState, ModelData childData) {
            this.facadePos = pos;
            this.childData = childData;
            this.setParent(parent);
            this.setPos(pos);
            this.setState(facadeState);
            this.setBlockEntity(null);
        }

        @Override
        public ModelData getModelData(BlockPos pos) {
            return this.facadePos.equals(pos) ? this.childData : this.parent.getModelData(pos);
        }
    }

    private record PreparedFacade(BlockStateModelPart part, Object key) {}

    private record BackPlateGeometry(List<BakedQuad> back, Map<Direction, List<BakedQuad>> culled,
                                     BackPlateKey key) {}

    private record BackPlateKey(Direction attachedSide, double thickness, Set<Direction> occupiedFaces,
                                List<BakedQuad> back, Map<Direction, List<BakedQuad>> culled) {}

    private record BackPlatePart(List<BakedQuad> back, Map<Direction, List<BakedQuad>> culled,
                                 TextureAtlasSprite sprite) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? this.back : this.culled.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return new Material.Baked(this.sprite, false);
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            int flags = 0;
            for (BakedQuad quad : this.back) flags |= quad.materialInfo().flags();
            for (List<BakedQuad> quads : this.culled.values()) {
                for (BakedQuad quad : quads) flags |= quad.materialInfo().flags();
            }
            return flags;
        }
    }

    private record FacadeGeometryKey(Direction side, BlockState state, boolean shouldRenderPlate,
                                     boolean shouldRenderBackSide, @Nullable Object modelKey,
                                     List<BakedQuad> unculled, Map<Direction, List<BakedQuad>> culled) {}

    private record ClippedPart(BlockStateModelPart parent, List<BakedQuad> unculled,
                               Map<Direction, List<BakedQuad>> culled, Direction side,
                               boolean shouldRenderBackSide) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction == null) return this.shouldRenderBackSide ? this.unculled : List.of();
            if (direction != this.side) return List.of();
            return this.culled.getOrDefault(direction, List.of());
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


