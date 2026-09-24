package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.List;

/** Registers the first fluid handler supplied by an item's components with NeoForge's item capability. */
public final class CombinedCapabilityProvider {

    private CombinedCapabilityProvider() {}

    public static void registerFluidItem(RegisterCapabilitiesEvent event, Item item, Iterable<?> components) {
        List<IComponentCapability> providers = new ArrayList<>();
        for (Object component : components) {
            if (component instanceof IComponentCapability provider) {
                providers.add(provider);
            }
        }
        if (providers.isEmpty() || event.isItemRegistered(Capabilities.Fluid.ITEM, item)) {
            return;
        }

        event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> {
            ItemAccess oneByOne = access.oneByOne();
            for (IComponentCapability provider : providers) {
                ResourceHandler<FluidResource> handler = provider.createFluidHandler(oneByOne);
                if (handler != null) {
                    return handler;
                }
            }
            return null;
        }, item);
    }
}
