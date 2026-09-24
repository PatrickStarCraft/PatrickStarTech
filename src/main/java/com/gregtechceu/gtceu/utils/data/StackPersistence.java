package com.gregtechceu.gtceu.utils.data;

import com.gregtechceu.gtceu.api.sync_system.ValueIOPersistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/** Component-preserving stack payloads for GT-owned saves, including oversized item counts. */
public final class StackPersistence {
    private StackPersistence() {}

    public static CompoundTag saveItem(ItemStack stack) {
        return saveItem(stack, ValueIOPersistence.builtInRegistries());
    }

    public static CompoundTag saveItem(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new CompoundTag();
        var tag = (CompoundTag) ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                stack.copyWithCount(1)).getOrThrow();
        tag.putInt("count", stack.getCount());
        return tag;
    }

    public static ItemStack loadItem(CompoundTag tag) {
        return loadItem(tag, ValueIOPersistence.builtInRegistries());
    }

    public static ItemStack loadItem(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) return ItemStack.EMPTY;
        if (tag.contains("Count") || tag.contains("tag")) {
            throw new IllegalArgumentException("Legacy item NBT needs data-component conversion before loading");
        }
        int count = tag.getIntOr("count", 1);
        if (count < 1) throw new IllegalArgumentException("Non-positive saved item count: " + count);
        var normalized = tag.copy();
        normalized.putInt("count", 1);
        var stack = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), normalized).getOrThrow();
        stack.setCount(count);
        return stack;
    }

    public static CompoundTag saveFluid(FluidStack stack) {
        return saveFluid(stack, ValueIOPersistence.builtInRegistries());
    }

    public static CompoundTag saveFluid(FluidStack stack, HolderLookup.Provider registries) {
        return (CompoundTag) FluidStack.OPTIONAL_CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    public static FluidStack loadFluid(CompoundTag tag) {
        return loadFluid(tag, ValueIOPersistence.builtInRegistries());
    }

    public static FluidStack loadFluid(CompoundTag tag, HolderLookup.Provider registries) {
        return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    /** Decode untrusted item data without turning a malformed fluid payload into a caller crash. */
    public static Optional<FluidStack> tryLoadFluid(CompoundTag tag) {
        return tryLoadFluid(tag, ValueIOPersistence.builtInRegistries());
    }

    public static Optional<FluidStack> tryLoadFluid(CompoundTag tag, HolderLookup.Provider registries) {
        return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result();
    }
}
