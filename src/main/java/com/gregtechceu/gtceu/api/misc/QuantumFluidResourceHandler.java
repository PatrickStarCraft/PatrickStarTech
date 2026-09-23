package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.data.StackPersistence;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * Transactional fluid access for quantum tank items. The transfer API accepts int-sized operations,
 * while this handler retains the tank's long-sized stored amount and capacity between operations.
 */
public final class QuantumFluidResourceHandler implements ResourceHandler<FluidResource> {

    private static final String STORED_KEY = "stored";
    private static final String STORED_AMOUNT_KEY = "storedAmount";

    private final ItemAccess itemAccess;
    private final Item validItem;
    private final long capacity;

    public QuantumFluidResourceHandler(ItemAccess itemAccess, long capacity) {
        this.itemAccess = Objects.requireNonNull(itemAccess);
        this.validItem = itemAccess.getResource().getItem();
        this.capacity = Math.max(0L, capacity);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        return readStored(itemAccess.getResource()).resource();
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return readStored(itemAccess.getResource()).amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        if (!isCurrentItem()) {
            return 0L;
        }
        return resource.isEmpty() || isValid(index, resource) ? capacity : 0L;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return isCurrentItem();
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !isCurrentItem() || itemAccess.getAmount() != 1) {
            return 0;
        }

        ItemResource currentItem = itemAccess.getResource();
        StoredFluid current = readStored(currentItem);
        if (!current.resource().isEmpty() && !current.resource().equals(resource)) {
            return 0;
        }

        long currentAmount = current.resource().isEmpty() ? 0L : current.amount();
        long room = capacity - currentAmount;
        if (room <= 0L) {
            return 0;
        }

        int inserted = (int) Math.min((long) amount, room);
        long newAmount = currentAmount + inserted;
        ItemResource updated = withStoredFluid(currentItem, resource, newAmount);
        return itemAccess.exchange(updated, 1, transaction) == 1 ? inserted : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || !isCurrentItem() || itemAccess.getAmount() != 1) {
            return 0;
        }

        ItemResource currentItem = itemAccess.getResource();
        StoredFluid current = readStored(currentItem);
        if (current.resource().isEmpty() || !current.resource().equals(resource)) {
            return 0;
        }

        int extracted = (int) Math.min((long) amount, current.amount());
        if (extracted == 0) {
            return 0;
        }

        ItemResource updated = withStoredFluid(currentItem, current.resource(), current.amount() - extracted);
        return itemAccess.exchange(updated, 1, transaction) == 1 ? extracted : 0;
    }

    private boolean isCurrentItem() {
        return itemAccess.getAmount() > 0 && itemAccess.getResource().is(validItem);
    }

    private StoredFluid readStored(ItemResource itemResource) {
        if (!itemResource.is(validItem)) {
            return StoredFluid.EMPTY;
        }
        CustomData customData = itemResource.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(STORED_KEY) || !customData.contains(STORED_AMOUNT_KEY)) {
            return StoredFluid.EMPTY;
        }

        CompoundTag root = customData.copyTag();
        FluidStack fluid = StackPersistence.loadFluid(root.getCompoundOrEmpty(STORED_KEY));
        long amount = root.getLongOr(STORED_AMOUNT_KEY, 0L);
        if (fluid.isEmpty() || amount <= 0L) {
            return StoredFluid.EMPTY;
        }

        fluid.setAmount(GTMath.saturatedCast(amount));
        return new StoredFluid(FluidResource.of(fluid), amount);
    }

    private static ItemResource withStoredFluid(ItemResource itemResource, FluidResource resource, long amount) {
        CustomData customData = itemResource.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        if (amount <= 0L || resource.isEmpty()) {
            root.remove(STORED_KEY);
            root.remove(STORED_AMOUNT_KEY);
        } else {
            FluidStack fluid = resource.toStack(GTMath.saturatedCast(amount));
            root.put(STORED_KEY, StackPersistence.saveFluid(fluid));
            root.putLong(STORED_AMOUNT_KEY, amount);
        }
        return itemResource.with(DataComponents.CUSTOM_DATA, root.isEmpty() ? null : CustomData.of(root));
    }

    private record StoredFluid(FluidResource resource, long amount) {
        private static final StoredFluid EMPTY = new StoredFluid(FluidResource.EMPTY, 0L);
    }
}
