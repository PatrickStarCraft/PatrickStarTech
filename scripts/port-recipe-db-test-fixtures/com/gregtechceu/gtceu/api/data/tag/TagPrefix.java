package com.gregtechceu.gtceu.api.data.tag;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

public final class TagPrefix {

    public static final Map<TagPrefix, Boolean> ORES = Map.of();

    public List<TagKey<Item>> getItemTags(Object material) {
        return List.of();
    }

    public List<TagKey<Item>> getItemParentTags() {
        return List.of();
    }
}
