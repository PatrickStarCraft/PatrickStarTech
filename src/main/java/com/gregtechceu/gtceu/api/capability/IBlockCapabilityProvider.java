package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/** Mod-owned dispatch used by the providers registered with NeoForge. */
public interface IBlockCapabilityProvider {
    <T> @Nullable T getGTCapability(BlockCapability<T, Direction> capability, @Nullable Direction side);
}
