package com.gregtechceu.gtceu.client.color;

import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IMaterialPartItem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item tint source for the material-dependent color of GregTech material parts. */
public record MaterialPartTintSource() implements ItemTintSource {

    public static final MapCodec<MaterialPartTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.point(new MaterialPartTintSource()));
    public static final Identifier ID = Identifier.fromNamespaceAndPath("gtceu", "material_part");

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (itemStack.getItem() instanceof IComponentItem componentItem) {
            for (var component : componentItem.getComponents()) {
                if (component instanceof IMaterialPartItem materialPartItem) {
                    return materialPartItem.getPartMaterial(itemStack).getMaterialARGB();
                }
            }
        }
        return -1;
    }

    @Override
    public MapCodec<MaterialPartTintSource> type() {
        return MAP_CODEC;
    }
}
