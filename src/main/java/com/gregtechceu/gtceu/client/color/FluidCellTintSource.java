package com.gregtechceu.gtceu.client.color;

import com.gregtechceu.gtceu.utils.GTUtil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/** Item tint source for the fluid layer of GregTech fluid containers. */
public record FluidCellTintSource() implements ItemTintSource {

    public static final MapCodec<FluidCellTintSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.point(new FluidCellTintSource()));
    public static final Identifier ID = Identifier.fromNamespaceAndPath("gtceu", "fluid_cell");

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return FluidUtil.getFluidContained(itemStack)
                .map(fluid -> fluid.getFluid() == Fluids.LAVA ? 0xFFFF7000 : GTUtil.getFluidColor(fluid))
                .orElse(-1);
    }

    @Override
    public MapCodec<FluidCellTintSource> type() {
        return MAP_CODEC;
    }
}
