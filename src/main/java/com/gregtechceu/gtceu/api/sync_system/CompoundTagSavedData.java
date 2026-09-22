package com.gregtechceu.gtceu.api.sync_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Codec-backed saved data for GT systems that own their compound-tag schema.
 * The codec delegates to that schema, rather than discarding fields during the SavedData API migration.
 */
public abstract class CompoundTagSavedData extends SavedData {

    public abstract CompoundTag save(CompoundTag tag);

    public static <T extends CompoundTagSavedData> SavedDataType<T> type(
            Function<CompoundTag, T> load, Supplier<T> create, String name) {
        return new SavedDataType<>(Identifier.fromNamespaceAndPath("gtceu", name), create,
                CompoundTag.CODEC.xmap(load, value -> value.save(new CompoundTag())));
    }
}
