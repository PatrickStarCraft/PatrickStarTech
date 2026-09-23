package com.gregtechceu.gtceu.client.color;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.MaterialBlockItem;
import com.gregtechceu.gtceu.api.item.MaterialPipeBlockItem;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.item.component.IMaterialPartItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Tint source for GT items whose color is selected by a material layer. */
public record MaterialLayerTintSource(int layer) implements ItemTintSource {

    public static final MapCodec<MaterialLayerTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.INT.fieldOf("layer").forGetter(MaterialLayerTintSource::layer))
                    .apply(instance, MaterialLayerTintSource::new));
    public static final Identifier ID = GTCEu.id("material_layer");

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (itemStack.getItem() instanceof TagPrefixItem materialItem) {
            return materialItem.material.getLayerARGB(this.layer);
        }
        if (itemStack.getItem() instanceof MaterialBlockItem materialBlockItem) {
            return materialBlockItem.material.getLayerARGB(this.layer);
        }
        if (itemStack.getItem() instanceof MaterialPipeBlockItem materialPipeItem) {
            var block = materialPipeItem.getBlock();
            return block.tinted(block.defaultBlockState(), null, null, this.layer);
        }
        if (itemStack.getItem() instanceof IComponentItem componentItem) {
            for (var component : componentItem.getComponents()) {
                if (component instanceof IMaterialPartItem materialPartItem) {
                    return materialPartItem.getPartMaterial(itemStack).getLayerARGB(this.layer);
                }
            }
        }
        return -1;
    }

    @Override
    public MapCodec<MaterialLayerTintSource> type() {
        return MAP_CODEC;
    }
}
