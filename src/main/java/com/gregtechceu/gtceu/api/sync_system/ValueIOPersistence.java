package com.gregtechceu.gtceu.api.sync_system;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/** Registry-aware bridge between GT's managed fields and NeoForge's ValueIO persistence. */
public final class ValueIOPersistence {
    private ValueIOPersistence() {}

    public static HolderLookup.Provider builtInRegistries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static CompoundTag write(ValueIOSerializable value, HolderLookup.Provider registries) {
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, registries);
        value.serialize(output);
        if (!problems.isEmpty()) throw new IllegalArgumentException(problems.getTreeReport());
        return output.buildResult();
    }

    /** Decode into a temporary value when the caller needs failure-atomic loading. */
    public static void read(ValueIOSerializable value, CompoundTag tag, HolderLookup.Provider registries) {
        var problems = new ProblemReporter.Collector();
        value.deserialize(TagValueInput.create(problems, registries, tag));
        if (!problems.isEmpty()) throw new IllegalArgumentException(problems.getTreeReport());
    }
}
