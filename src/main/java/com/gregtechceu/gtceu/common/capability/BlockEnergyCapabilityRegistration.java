package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.capability.BlockEnergyCapabilities;
import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.capability.IBlockCapabilityProvider;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.block.CableBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.HashSet;

public final class BlockEnergyCapabilityRegistration {

    private BlockEnergyCapabilityRegistration() {}

    public static void register(RegisterCapabilitiesEvent event) {
        var blocks = new HashSet<Block>();
        GTRegistries.MACHINES.values().forEach(definition -> blocks.add(definition.getBlock()));
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof PipeBlock<?, ?, ?>) blocks.add(block);
        }
        for (Block block : blocks) {
            BlockEnergyCapabilities.register(event, block);
            register(event, block, GTCapability.CAPABILITY_COVERABLE);
            register(event, block, GTCapability.CAPABILITY_WORKABLE);
            register(event, block, GTCapability.CAPABILITY_CONTROLLABLE);
            register(event, block, GTCapability.CAPABILITY_ENERGY_INFO_PROVIDER);
            register(event, block, GTCapability.CAPABILITY_LASER);
            register(event, block, GTCapability.CAPABILITY_DATA_ACCESS);
            register(event, block, GTCapability.CAPABILITY_COMPUTATION_PROVIDER);
            register(event, block, GTCapability.CAPABILITY_HAZARD_CONTAINER);
            register(event, block, GTCapability.CAPABILITY_MONITOR_COMPONENT);
            register(event, block, com.gregtechceu.gtceu.api.capability.GTTransferCapabilities.ITEM);
            register(event, block, com.gregtechceu.gtceu.api.capability.GTTransferCapabilities.FLUID);
            register(event, block, com.gregtechceu.gtceu.api.capability.GTTransferCapabilities.ENERGY);
        }
    }

    private static <T> void register(RegisterCapabilitiesEvent event, Block block, BlockCapability<T, Direction> capability) {
        event.registerBlock(capability, (level, pos, state, entity, side) ->
                entity != null && !entity.isRemoved() && entity instanceof IBlockCapabilityProvider provider ?
                        provider.getGTCapability(capability, side) : null, block);
    }
}
