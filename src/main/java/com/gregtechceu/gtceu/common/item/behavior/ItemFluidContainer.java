package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.component.IRecipeRemainder;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ItemFluidContainer implements IRecipeRemainder {

    @Override
    public ItemStack getRecipeRemained(ItemStack itemStack) {
        if (itemStack.isEmpty()) return itemStack;

        ResourceHandler<FluidResource> handler = ItemAccess.forStack(itemStack).oneByOne()
                .getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return itemStack;

        for (int i = 0; i < handler.size(); i++) {
            FluidResource fluid = handler.getResource(i);
            if (fluid.isEmpty()) continue;

            try (Transaction transaction = Transaction.openRoot()) {
                int drained = handler.extract(i, fluid, FluidType.BUCKET_VOLUME, transaction);
                if (drained == FluidType.BUCKET_VOLUME) {
                    transaction.commit();
                }
            }
            // As with the old item handler, finding a fluid handler always consumes the crafting input.
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }
}
