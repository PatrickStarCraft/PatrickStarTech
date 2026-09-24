package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.neoforged.neoforge.model.data.ModelData;

public interface ICoverRenderer {

    default ModelData getModelData(CoverBehavior coverBehavior, BlockPos pos, BlockAndTintGetter level,
                                   ModelData holderModelData) {
        return ModelData.EMPTY;
    }

}
