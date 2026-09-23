package com.gregtechceu.gtceu.utils.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import com.mojang.serialization.DynamicOps;

/** Reads current BlockPos codec data while retaining the legacy X/Y/Z compound format. */
public final class BlockPosNbt {
    private BlockPosNbt() {}

    public static BlockPos read(Tag tag, DynamicOps<Tag> ops) {
        if (tag instanceof CompoundTag compound) {
            return readLegacy(compound);
        }

        return BlockPos.CODEC.parse(ops, tag).getOrThrow();
    }

    public static BlockPos readLegacy(CompoundTag compound) {
        return new BlockPos(
                compound.getInt("X").orElse(0),
                compound.getInt("Y").orElse(0),
                compound.getInt("Z").orElse(0));
    }

    public static CompoundTag writeLegacy(BlockPos pos) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", pos.getX());
        compound.putInt("Y", pos.getY());
        compound.putInt("Z", pos.getZ());
        return compound;
    }
}
