package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

/** Compile-only helper for PlungerBehavior's untested block-lookup path. */
public final class GTCapabilityHelper {

    private GTCapabilityHelper() {}

    public static @Nullable IFluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return null;
    }
}
