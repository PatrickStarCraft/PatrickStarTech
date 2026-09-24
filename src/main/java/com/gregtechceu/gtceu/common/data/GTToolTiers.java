package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.world.level.block.state.BlockState;

public final class GTToolTiers {

    private GTToolTiers() {}

    public static boolean isCorrectForDrops(BlockState state, int harvestLevel) {
        for (int requiredTier = 0; requiredTier < CustomTags.TOOL_TIERS.length; requiredTier++) {
            if (harvestLevel < requiredTier && state.is(CustomTags.TOOL_TIERS[requiredTier])) {
                return false;
            }
        }
        return true;
    }
}
