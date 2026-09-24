package com.gregtechceu.gtceu.api.capability.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public abstract class CapabilityCompatProvider {

    private final BlockEntity blockEntity;

    protected CapabilityCompatProvider(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    protected <T> @Nullable T getUpvalueCapability(BlockCapability<T, @Nullable Direction> capability,
                                                    @Nullable Direction facing) {
        Level level = blockEntity.getLevel();
        if (level == null || blockEntity.isRemoved()) {
            return null;
        }
        return level.getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity,
                facing);
    }
}
