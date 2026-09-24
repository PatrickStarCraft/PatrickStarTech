package com.gregtechceu.gtceu.api.item.component.forge;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jetbrains.annotations.Nullable;

public interface IComponentCapability {

    @Nullable ResourceHandler<FluidResource> createFluidHandler(ItemAccess access);
}
