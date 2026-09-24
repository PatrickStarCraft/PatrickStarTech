package com.gregtechceu.gtceu.api.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Compile-only machine view for the unused SpoilContext constructor. */
public interface MetaMachine {

    Level getLevel();
    BlockPos getBlockPos();
}
