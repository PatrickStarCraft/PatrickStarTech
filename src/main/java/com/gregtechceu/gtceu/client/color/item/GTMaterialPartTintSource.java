package com.gregtechceu.gtceu.client.color.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.component.IMaterialPartItem;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class GTMaterialPartTintSource implements ItemTintSource {

    public static final Identifier ID = GTCEu.id("material_part");
    public static final GTMaterialPartTintSource INSTANCE = new GTMaterialPartTintSource();
    public static final MapCodec<GTMaterialPartTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private GTMaterialPartTintSource() {}

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return IMaterialPartItem.getItemStackColor(itemStack);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
