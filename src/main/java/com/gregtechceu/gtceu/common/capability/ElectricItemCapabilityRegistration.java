package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.capability.ElectricItemCapabilities;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.capability.ElectricItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ElectricItemCapabilityRegistration {

    private ElectricItemCapabilityRegistration() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // The capability event runs after item registration. Include addon implementations too.
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof IGTTool tool && tool.isElectric()) {
                ElectricItemCapabilities.register(event, item,
                        stack -> new ElectricItem(stack, 0L, tool.getElectricTier(), true, false));
            } else if (item instanceof IComponentItem componentItem) {
                ElectricItemCapabilities.register(event, item, stack -> {
                    // Resolve in attachment order, matching the former first-present component lookup.
                    for (var component : componentItem.getComponents()) {
                        if (component instanceof ElectricStats stats) {
                            return stats.createElectricItem(stack);
                        }
                    }
                    return null;
                });
            }
        }
    }
}
