package com.gregtechceu.gtceu.client.color;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/** Applies the registered machine builder's per-stack tint to an item model layer. */
public record MachineItemTintSource(int layer) implements ItemTintSource {

    public static final Identifier ID = GTCEu.id("machine_item");
    public static final MapCodec<MachineItemTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.INT.fieldOf("layer").forGetter(MachineItemTintSource::layer))
                    .apply(instance, MachineItemTintSource::new));

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return itemStack.getItem() instanceof MetaMachineItem machineItem ?
                machineItem.getTintColor(itemStack, this.layer) : -1;
    }

    @Override
    public MapCodec<MachineItemTintSource> type() {
        return MAP_CODEC;
    }
}
