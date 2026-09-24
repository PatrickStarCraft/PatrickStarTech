package com.gregtechceu.gtceu.client.model.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/** Immutable controller snapshot copied onto a formed part for chunk-mesh model selection. */
public record ControllerPartRenderState(BlockState controllerBlockState, BlockPos controllerPos,
                                        Direction frontFacing, Direction upwardsFacing,
                                        boolean flipped, boolean active) {
    public ControllerPartRenderState {
        Objects.requireNonNull(controllerBlockState, "controllerBlockState");
        controllerPos = Objects.requireNonNull(controllerPos, "controllerPos").immutable();
        Objects.requireNonNull(frontFacing, "frontFacing");
        Objects.requireNonNull(upwardsFacing, "upwardsFacing");
    }
}
