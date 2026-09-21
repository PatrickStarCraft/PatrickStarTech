package com.gregtechceu.gtceu.api.capability;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Function;

/** Electric handlers are short-lived views of the queried stack, never copies or cached item singletons. */
public final class ElectricItemCapabilities {

    public static final ItemCapability<IElectricItem, Void> ELECTRIC_ITEM = ItemCapability.createVoid(
            Identifier.fromNamespaceAndPath("gtceu", "electric_item"), IElectricItem.class);

    private ElectricItemCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event, Item item,
                                Function<ItemStack, IElectricItem> factory) {
        event.registerItem(ELECTRIC_ITEM, (stack, context) -> factory.apply(stack), item);
    }
}
