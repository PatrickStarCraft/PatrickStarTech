package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;

import net.minecraft.nbt.Tag;

/** Compile-only merge contract for the selected spoilable-stack source. */
public interface IMergeableNBTSerializable extends NBTSerializable<Tag> {

    void prepareForComparisonWith(NBTSerializable<Tag> other);
}
