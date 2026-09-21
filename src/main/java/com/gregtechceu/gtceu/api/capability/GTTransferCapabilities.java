package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT's non-transactional transfer boundary during migration. These are deliberately GT capabilities,
 * not NeoForge's transactional capabilities: pipe routing and cover callbacks cannot be rolled back
 * by snapshotting a single inventory. Native-to-GT automation still requires transactional providers.
 */
public final class GTTransferCapabilities {
    public static final BlockCapability<IItemHandler, Direction> ITEM = BlockCapability.createSided(
            Identifier.fromNamespaceAndPath("gtceu", "legacy_item_handler"), IItemHandler.class);
    public static final BlockCapability<IFluidHandler, Direction> FLUID = BlockCapability.createSided(
            Identifier.fromNamespaceAndPath("gtceu", "legacy_fluid_handler"), IFluidHandler.class);
    public static final BlockCapability<IEnergyStorage, Direction> ENERGY = BlockCapability.createSided(
            Identifier.fromNamespaceAndPath("gtceu", "legacy_energy_handler"), IEnergyStorage.class);

    private GTTransferCapabilities() {}
}
