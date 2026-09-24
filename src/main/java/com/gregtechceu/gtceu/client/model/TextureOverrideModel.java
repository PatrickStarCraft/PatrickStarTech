package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.client.util.quad.transformers.GTQuadTransformers;
import com.gregtechceu.gtceu.core.util.extensions.BakedQuadExt;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Applies texture-key overrides to each target-version block model part. */
public class TextureOverrideModel<T extends BlockStateModel> implements BlockStateModel {

    public static final UnaryOperator<BakedQuad> OVERLAY_OFFSET = GTQuadTransformers.offset(0.002f);

    @Getter
    protected final T originalModel;

    @Getter
    protected final Map<String, TextureAtlasSprite> textureOverrides;

    public TextureOverrideModel(T child, Map<String, TextureAtlasSprite> textureOverrides) {
        this.originalModel = child;
        this.textureOverrides = Map.copyOf(textureOverrides);
    }

    public BlockStateModel getChild() {
        return this.originalModel;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        this.originalModel.collectParts(random, parts);
        this.addRetexturedParts(parts, output);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        this.originalModel.collectParts(level, pos, state, random, parts);
        this.addRetexturedParts(parts, output);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                               RandomSource random) {
        return new GeometryKey(this.originalModel,
                this.originalModel.createGeometryKey(level, pos, state, random), this.textureOverrides);
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

    private void addRetexturedParts(List<BlockStateModelPart> parts, List<BlockStateModelPart> output) {
        for (BlockStateModelPart part : parts) {
            output.add(new RetexturedPart(part, this.textureOverrides));
        }
    }

    public static List<BakedQuad> retextureQuads(List<BakedQuad> quads, Map<String, TextureAtlasSprite> overrides) {
        List<BakedQuad> newQuads = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            String textureKey = ((BakedQuadExt) (Object) quad).gtceu$getTextureKey();
            if (textureKey == null || textureKey.isEmpty()) {
                newQuads.add(quad);
                continue;
            }
            if (textureKey.charAt(0) == '#') {
                textureKey = textureKey.substring(1);
            }

            TextureAtlasSprite replacement = overrides.get(textureKey);
            newQuads.add(replacement == null ? quad : GTQuadTransformers.setSprite(quad, replacement));
        }
        return newQuads;
    }

    private record RetexturedPart(BlockStateModelPart original,
                                  Map<String, TextureAtlasSprite> overrides) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return retextureQuads(this.original.getQuads(direction), this.overrides);
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
        public @BakedQuad.MaterialFlags int materialFlags() {
            return this.original.materialFlags();
        }
    }

    private record GeometryKey(BlockStateModel child, @Nullable Object childKey,
                               Map<String, TextureAtlasSprite> overrides) {}
}
