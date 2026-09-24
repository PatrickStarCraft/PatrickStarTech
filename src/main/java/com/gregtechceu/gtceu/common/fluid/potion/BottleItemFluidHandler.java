package com.gregtechceu.gtceu.common.fluid.potion;

import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Transactional fluid access for glass bottles and the potion items they become when filled. */
public class BottleItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {

    private static final int CAPACITY = PotionFluidHelper.BOTTLE_AMOUNT;
    private final DataComponentType<SimpleFluidContent> fluidContent;

    public BottleItemFluidHandler(ItemAccess access) {
        super(access, 1);
        this.fluidContent = GTDataComponents.FLUID_CONTENT.get();
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (itemAccess.getAmount() != 1 || !accessResource.is(Items.GLASS_BOTTLE)) {
            return FluidResource.EMPTY;
        }
        return FluidResource.of(accessResource.getOrDefault(fluidContent, SimpleFluidContent.EMPTY).copy());
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        if (itemAccess.getAmount() != 1 || !accessResource.is(Items.GLASS_BOTTLE)) {
            return 0;
        }
        return accessResource.getOrDefault(fluidContent, SimpleFluidContent.EMPTY).getAmount();
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (!accessResource.is(Items.GLASS_BOTTLE)) {
            return ItemResource.EMPTY;
        }
        if (newAmount == 0) {
            return accessResource.with(fluidContent, null);
        }
        if (newAmount != CAPACITY) {
            return ItemResource.EMPTY;
        }

        FluidStack fluid = newResource.toStack(newAmount);
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS,
                fluid.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY));
        return ItemResource.of(potion);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return itemAccess.getAmount() == 1 && itemAccess.getResource().is(Items.GLASS_BOTTLE) &&
                (resource.isEmpty() || resource.toStack(1).getFluid().is(CustomTags.POTION_FLUIDS));
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return CAPACITY;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return itemAccess.getAmount() == 1 && itemAccess.getResource().is(Items.GLASS_BOTTLE) ?
                super.getCapacityAsLong(index, resource) : 0;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (itemAccess.getAmount() != 1 || getAmountFrom(itemAccess.getResource(), index) != 0 ||
                amount < CAPACITY || !isValid(index, resource)) {
            return 0;
        }
        return super.insert(index, resource, CAPACITY, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (itemAccess.getAmount() != 1 || amount < CAPACITY ||
                getAmountFrom(itemAccess.getResource(), index) != CAPACITY) {
            return 0;
        }
        return super.extract(index, resource, CAPACITY, transaction);
    }
}
