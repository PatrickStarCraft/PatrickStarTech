package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public final class BlockEnergyCapabilities {

    public static final BlockCapability<IEnergyContainer, Direction> ENERGY_CONTAINER = BlockCapability.createSided(
            Identifier.fromNamespaceAndPath("gtceu", "energy_container"), IEnergyContainer.class);

    private BlockEnergyCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event, Block block) {
        event.registerBlock(ENERGY_CONTAINER,
                (level, pos, state, entity, side) -> resolve(entity, side), block);
    }

    /** Never expose removed block entities, including during replacement/unload. */
    public static @Nullable IEnergyContainer resolve(@Nullable BlockEntity entity, @Nullable Direction side) {
        return entity != null && !entity.isRemoved() && entity instanceof IBlockEnergyProvider provider ?
                provider.getEnergyContainer(side) : null;
    }
}
