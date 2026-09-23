package com.gregtechceu.gtceu.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * @deprecated Use normal Block class instead - replace {@code AppearanceBlock::getBlockAppearance} with
 *             {@code Block::getAppearance}
 */
@Deprecated(forRemoval = true)
public class AppearanceBlock extends Block implements IAppearance {

    public AppearanceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndLightGetter level, BlockPos pos, Direction side,
                                    @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        if (!(level instanceof BlockAndTintGetter tintGetter)) return state;
        var appearance = this.getBlockAppearance(state, tintGetter, pos, side, queryState, queryPos);
        return appearance == null ? state : appearance;
    }
}
