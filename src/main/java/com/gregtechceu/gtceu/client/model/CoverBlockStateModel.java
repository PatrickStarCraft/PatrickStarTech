package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.model.quad.StaticFaceBakery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Builds reload-scoped static overlay and backplate parts for ordinary machine and pipe covers. */
@OnlyIn(Dist.CLIENT)
public final class CoverBlockStateModel {

    private static final Identifier BACK_PLATE_TEXTURE = GTCEu.id("block/cover/cover_back_plate");

    private final Map<CoverRenderState, CoverParts> cache = new ConcurrentHashMap<>();

    public void collectParts(BlockAndTintGetter level, BlockPos pos, List<BlockStateModelPart> output) {
        CoverRenderState state = level.getModelData(pos).get(GTModelProperties.COVER_RENDER_STATE);
        if (state == null || state.faces().isEmpty()) return;
        output.addAll(this.parts(state).parts());
    }

    public Object geometryKey(BlockAndTintGetter level, BlockPos pos) {
        CoverRenderState state = level.getModelData(pos).get(GTModelProperties.COVER_RENDER_STATE);
        return state == null ? CoverRenderState.EMPTY : state;
    }

    public int materialFlags(BlockAndTintGetter level, BlockPos pos) {
        CoverRenderState state = level.getModelData(pos).get(GTModelProperties.COVER_RENDER_STATE);
        return state == null ? 0 : this.parts(state).materialFlags();
    }

    private CoverParts parts(CoverRenderState state) {
        return this.cache.computeIfAbsent(state, this::bakeParts);
    }

    private CoverParts bakeParts(CoverRenderState state) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
        List<BakedQuad> unculled = new ArrayList<>();
        EnumMap<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
        @Nullable TextureAtlasSprite particleSprite = null;
        @Nullable TextureAtlasSprite backPlateSprite = null;

        for (Direction attachedSide : Direction.values()) {
            CoverRenderState.CoverFace cover = state.faces().get(attachedSide);
            if (cover == null) continue;

            if (cover.overlay() != null) {
                TextureAtlasSprite overlay = atlas.getSprite(cover.overlay());
                particleSprite = particleSprite == null ? overlay : particleSprite;
                BakedQuad overlayQuad = StaticFaceBakery.bakeFace(StaticFaceBakery.COVER_OVERLAY,
                        attachedSide, overlay);
                unculled.add(overlayQuad);
                culled.computeIfAbsent(attachedSide, $ -> new ArrayList<>()).add(overlayQuad);
            }
            if (cover.emissiveOverlay() != null) {
                if (!cover.optionalEmissive() || atlas.getTextures().containsKey(cover.emissiveOverlay())) {
                    TextureAtlasSprite overlay = atlas.getSprite(cover.emissiveOverlay());
                    particleSprite = particleSprite == null ? overlay : particleSprite;
                    BakedQuad overlayQuad = cover.explicitEmissive()
                            ? StaticFaceBakery.bakeFace(StaticFaceBakery.COVER_OVERLAY, attachedSide, overlay,
                                    BlockModelRotation.IDENTITY, -101, 15, true, false)
                            : StaticFaceBakery.bakeFace(StaticFaceBakery.COVER_OVERLAY, attachedSide, overlay);
                    unculled.add(overlayQuad);
                    culled.computeIfAbsent(attachedSide, $ -> new ArrayList<>()).add(overlayQuad);
                }
            }

            if (!cover.renderPlate() || state.plateThickness() <= 0.0) continue;
            if (backPlateSprite == null) backPlateSprite = atlas.getSprite(BACK_PLATE_TEXTURE);

            AABB plate = plateBounds(attachedSide, state.plateThickness());
            unculled.add(StaticFaceBakery.bakeFace(plate, attachedSide.getOpposite(), backPlateSprite, true));
            for (Direction side : Direction.values()) {
                if (side == attachedSide.getOpposite()) continue;
                boolean occupied = (state.occupiedFaces() & 1 << side.ordinal()) != 0;
                if (!occupied || side == attachedSide) {
                    culled.computeIfAbsent(side, $ -> new ArrayList<>())
                            .add(StaticFaceBakery.bakeFace(plate, side, backPlateSprite, true));
                }
            }
        }

        if (particleSprite == null) particleSprite = backPlateSprite;
        if (particleSprite == null) return CoverParts.EMPTY;

        EnumMap<Direction, List<BakedQuad>> immutableCulled = new EnumMap<>(Direction.class);
        culled.forEach((direction, quads) -> immutableCulled.put(direction, List.copyOf(quads)));
        List<BakedQuad> immutableUnculled = List.copyOf(unculled);
        int flags = 0;
        for (BakedQuad quad : immutableUnculled) flags |= quad.materialInfo().flags();
        for (List<BakedQuad> quads : immutableCulled.values()) {
            for (BakedQuad quad : quads) flags |= quad.materialInfo().flags();
        }

        BlockStateModelPart part = new CoverPart(immutableUnculled, Map.copyOf(immutableCulled),
                new Material.Baked(particleSprite, false), flags);
        return new CoverParts(List.of(part), flags);
    }

    private static AABB plateBounds(Direction attachedSide, double thickness) {
        return switch (attachedSide) {
            case DOWN -> StaticFaceBakery.COVER_OVERLAY.setMaxY(thickness);
            case UP -> StaticFaceBakery.COVER_OVERLAY.setMinY(1.0 - thickness);
            case NORTH -> StaticFaceBakery.COVER_OVERLAY.setMaxZ(thickness);
            case SOUTH -> StaticFaceBakery.COVER_OVERLAY.setMinZ(1.0 - thickness);
            case WEST -> StaticFaceBakery.COVER_OVERLAY.setMaxX(thickness);
            case EAST -> StaticFaceBakery.COVER_OVERLAY.setMinX(1.0 - thickness);
        };
    }

    private record CoverParts(List<BlockStateModelPart> parts, int materialFlags) {
        private static final CoverParts EMPTY = new CoverParts(List.of(), 0);
    }

    private record CoverPart(List<BakedQuad> unculled, Map<Direction, List<BakedQuad>> culled,
                             Material.Baked particleMaterial, int materialFlags) implements BlockStateModelPart {

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? this.unculled : this.culled.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }
    }
}
