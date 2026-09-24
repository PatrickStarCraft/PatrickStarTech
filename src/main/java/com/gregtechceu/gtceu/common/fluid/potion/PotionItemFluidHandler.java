package com.gregtechceu.gtceu.common.fluid.potion;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional draining for potion items; a full bottle becomes a glass bottle. */
public class PotionItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {

    private static final int CAPACITY = PotionFluidHelper.BOTTLE_AMOUNT;

    public PotionItemFluidHandler(ItemAccess access) {
        super(access, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (itemAccess.getAmount() != 1 || !accessResource.is(Items.POTION)) {
            return FluidResource.EMPTY;
        }
        return FluidResource.of(PotionFluidHelper.getFluidFromPotionItem(accessResource.toStack(1), CAPACITY));
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        if (itemAccess.getAmount() != 1 || !accessResource.is(Items.POTION)) {
            return 0;
        }
        return PotionFluidHelper.getFluidFromPotionItem(accessResource.toStack(1), CAPACITY).getAmount();
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (accessResource.is(Items.POTION) && newAmount == 0) {
            return ItemResource.of(new ItemStack(Items.GLASS_BOTTLE));
        }
        return ItemResource.EMPTY;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return itemAccess.getAmount() == 1 && itemAccess.getResource().is(Items.POTION) &&
                resource.equals(getResourceFrom(itemAccess.getResource(), index));
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return CAPACITY;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return itemAccess.getAmount() == 1 && itemAccess.getResource().is(Items.POTION) ?
                super.getCapacityAsLong(index, resource) : 0;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (itemAccess.getAmount() != 1 || amount < CAPACITY ||
                getAmountFrom(itemAccess.getResource(), index) != CAPACITY ||
                !resource.equals(getResourceFrom(itemAccess.getResource(), index))) {
            return 0;
        }
        return super.extract(index, resource, CAPACITY, transaction);
    }
}
