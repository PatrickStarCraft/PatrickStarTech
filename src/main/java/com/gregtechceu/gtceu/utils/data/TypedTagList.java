package com.gregtechceu.gtceu.utils.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Preserve GT's typed-list validation when reading 26.2's potentially heterogeneous NBT lists. */
public final class TypedTagList {
    private TypedTagList() {}

    public static ListTag read(CompoundTag parent, String key, int elementType) {
        ListTag list = parent.getListOrEmpty(key);
        for (var entry : list) {
            if (entry.getId() != elementType) return new ListTag();
        }
        return list;
    }
}
