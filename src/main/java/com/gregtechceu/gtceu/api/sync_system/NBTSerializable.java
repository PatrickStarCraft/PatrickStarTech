package com.gregtechceu.gtceu.api.sync_system;

import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;

/**
 * Persistent state of an existing GT managed object. This is not a capability provider or a
 * Minecraft save hook; the managed sync system owns invoking these methods. Registry-dependent
 * values should use a ValueTransformer with its registry-aware context instead.
 */
public interface NBTSerializable<T extends Tag> {
    T serializeNBT();

    void deserializeNBT(T tag);

    default T serializeNBT(HolderLookup.Provider registries) {
        return serializeNBT();
    }

    default void deserializeNBT(T tag, HolderLookup.Provider registries) {
        deserializeNBT(tag);
    }
}
