package com.gregtechceu.gtceu.client.model.machine;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Supplies state-selected static geometry for formed controller parts without retaining live machine objects. */
public interface ControllerPartModel {

    PartModelResult collectPartModel(ControllerPartRenderState controller, BlockStateModelSet modelSet,
                                     BlockAndTintGetter level, BlockPos partPos, BlockState partState,
                                     RandomSource random);

    record PartModelResult(List<BlockStateModelPart> parts, Object geometryKey) {
        public PartModelResult {
            parts = List.copyOf(parts);
        }
    }
}
