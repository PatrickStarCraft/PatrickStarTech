package com.gregtechceu.gtceu.client.color;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.lang.reflect.Field;

/** Registers GregTech's item tint source codecs with vanilla's item model decoder. */
public final class GTItemTintSources {

    private GTItemTintSources() {}

    public static void bootstrap() {
        ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> mapper = getMapper();
        mapper.put(FluidCellTintSource.ID, FluidCellTintSource.MAP_CODEC);
        mapper.put(MaterialPartTintSource.ID, MaterialPartTintSource.MAP_CODEC);
    }

    @SuppressWarnings("unchecked")
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> getMapper() {
        try {
            Field mapperField = ItemTintSources.class.getDeclaredField("ID_MAPPER");
            mapperField.setAccessible(true);
            return (ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>>) mapperField.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access the item tint source registry", exception);
        }
    }
}
