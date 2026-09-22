package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.placeholder.Placeholder;

import org.jspecify.annotations.NullMarked;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import com.gregtechceu.gtceu.api.sync_system.CompoundTagSavedData;

import javax.annotation.ParametersAreNonnullByDefault;

@NullMarked
@ParametersAreNonnullByDefault
public class PlaceholderSavedData extends CompoundTagSavedData {

    private final ServerLevel level;
    private final CompoundTag tag;

    public static PlaceholderSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(com.gregtechceu.gtceu.api.sync_system.CompoundTagSavedData.type(
                tag -> new PlaceholderSavedData(level, tag),
                () -> new PlaceholderSavedData(level), "gtceu_placeholder_data"));
    }

    public PlaceholderSavedData(ServerLevel level) {
        this(level, new CompoundTag());
    }

    public PlaceholderSavedData(ServerLevel level, CompoundTag tag) {
        this.level = level;
        this.tag = tag.getCompoundOrEmpty("data");
    }

    public CompoundTag getPlaceholderData(Placeholder placeholder) {
        if (!tag.contains(placeholder.getName()))
            tag.put(placeholder.getName(), new CompoundTag());
        return tag.getCompound(placeholder.getName());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("data", this.tag);
        return tag;
    }
}
