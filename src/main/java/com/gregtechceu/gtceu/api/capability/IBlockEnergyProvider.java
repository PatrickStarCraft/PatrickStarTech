package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Implemented by block entities that expose GT EU through NeoForge's block capability system. */
public interface IBlockEnergyProvider {

    @Nullable IEnergyContainer getEnergyContainer(@Nullable Direction side);
}
