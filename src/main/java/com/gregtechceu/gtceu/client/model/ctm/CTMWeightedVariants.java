package com.gregtechceu.gtceu.client.model.ctm;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Preserves weighted model selection while exposing all choices to connected-texture discovery. */
public final class CTMWeightedVariants implements DynamicBlockStateModel, CTMModelPartSource {

    private final WeightedVariants weighted;
    private final List<BlockStateModel> candidates;

    private CTMWeightedVariants(WeightedVariants weighted, List<BlockStateModel> candidates) {
        this.weighted = weighted;
        this.candidates = List.copyOf(candidates);
    }

    public static BlockStateModel bake(BlockStateModel.Unbaked unbaked, ModelBaker baker) {
        if (unbaked instanceof WeightedVariants.Unbaked weighted) {
            WeightedList<BlockStateModel> entries = weighted.entries().map(model -> bake(model, baker));
            return new CTMWeightedVariants(new WeightedVariants(entries), entries.unwrap().stream()
                    .map(entry -> entry.value()).toList());
        }
        return unbaked.bake(baker);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        this.weighted.collectParts(random, output);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                             List<BlockStateModelPart> output) {
        this.weighted.collectParts(level, pos, state, random, output);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                               RandomSource random) {
        return this.weighted.createGeometryKey(level, pos, state, random);
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.weighted.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.weighted.particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.weighted.materialFlags();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return this.weighted.materialFlags(level, pos, state);
    }

    @Override
    public void collectConnectedTextureCandidates(RandomSource random, List<BlockStateModelPart> output) {
        long seed = random.nextLong();
        for (BlockStateModel candidate : this.candidates) {
            RandomSource candidateRandom = RandomSource.create(seed);
            if (candidate instanceof CTMModelPartSource source) {
                source.collectConnectedTextureCandidates(candidateRandom, output);
            } else {
                candidate.collectParts(candidateRandom, output);
            }
        }
    }
}
