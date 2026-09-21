package com.gregtechceu.gtceu.api.transfer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import com.gregtechceu.gtceu.api.sync_system.ValueIOPersistence;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;
import net.neoforged.neoforge.items.ItemStackHandler;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class CustomItemStackHandler extends ItemStackHandler
                                    implements NBTSerializable<CompoundTag> {

    @Getter
    @Setter
    protected @NotNull Runnable onContentsChanged = () -> {};
    @Getter
    @Setter
    protected Predicate<ItemStack> filter = stack -> true;

    public CustomItemStackHandler() {
        super();
    }

    public CustomItemStackHandler(int size) {
        super(size);
    }

    public CustomItemStackHandler(ItemStack itemStack) {
        this(NonNullList.of(ItemStack.EMPTY, itemStack));
    }

    public CustomItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return filter.test(stack);
    }

    @Override
    public void onContentsChanged(int slot) {
        onContentsChanged.run();
    }

    public void clear() {
        stacks.clear();
        onContentsChanged.run();
    }

    public NonNullList<ItemStack> toList() {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int slot = 0; slot < getSlots(); slot++) list.add(getStackInSlot(slot));
        return list;
    }

    public void dropInventoryInWorld(Level world, BlockPos pos) {
        for (ItemStack stack : stacks) {
            Block.popResource(world, pos, stack);
        }
        clear();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        deserializeNBT(nbt, ValueIOPersistence.builtInRegistries());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        var data = nbt.copy();
        data.putInt("Size", stacks.size());
        var loaded = new ItemStackHandler(stacks.size());
        ValueIOPersistence.read(loaded, data, registries);
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, loaded.getStackInSlot(i));
        onLoad();
        onContentsChanged.run();
    }

    @Override
    public CompoundTag serializeNBT() {
        return serializeNBT(ValueIOPersistence.builtInRegistries());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        return ValueIOPersistence.write(this, registries);
    }
}
