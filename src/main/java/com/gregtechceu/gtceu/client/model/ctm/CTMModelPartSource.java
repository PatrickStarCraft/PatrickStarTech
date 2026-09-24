package com.gregtechceu.gtceu.client.model.ctm;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Supplies model-bake candidates when a dynamic model selects child parts from block-entity data. */
public interface CTMModelPartSource {

    void collectConnectedTextureCandidates(RandomSource random, List<BlockStateModelPart> output);
}
