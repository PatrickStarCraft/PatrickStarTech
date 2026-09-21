package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.capability.BlockEnergyCapabilities;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.block.CableBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.HashSet;

public final class BlockEnergyCapabilityRegistration {

    private BlockEnergyCapabilityRegistration() {}

    public static void register(RegisterCapabilitiesEvent event) {
        var blocks = new HashSet<Block>();
        GTRegistries.MACHINES.values().forEach(definition -> blocks.add(definition.getBlock()));
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof CableBlock) blocks.add(block);
        }
        blocks.forEach(block -> BlockEnergyCapabilities.register(event, block));
    }
}
