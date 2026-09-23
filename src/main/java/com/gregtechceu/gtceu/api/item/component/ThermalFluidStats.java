package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidPipeProperties;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.misc.ThermalFluidResourceHandler;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.fluids.FluidUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThermalFluidStats implements IItemComponent, IAddInformation {

    public final int capacity;
    public final int maxFluidTemperature;
    public final boolean gasProof;
    public final boolean acidProof;
    public final boolean cryoProof;
    public final boolean plasmaProof;
    public final boolean allowPartialFill;

    protected ThermalFluidStats(int capacity, int maxFluidTemperature, boolean gasProof, boolean acidProof,
                                boolean cryoProof, boolean plasmaProof, boolean allowPartialFill) {
        this.capacity = capacity;
        this.maxFluidTemperature = maxFluidTemperature;
        this.gasProof = gasProof;
        this.acidProof = acidProof;
        this.cryoProof = cryoProof;
        this.plasmaProof = plasmaProof;
        this.allowPartialFill = allowPartialFill;
    }

    public static ThermalFluidStats create(int capacity, int maxFluidTemperature, boolean gasProof, boolean acidProof,
                                           boolean cryoProof, boolean plasmaProof, boolean allowPartialFill) {
        return new ThermalFluidStats(capacity, maxFluidTemperature, gasProof, acidProof, cryoProof, plasmaProof,
                allowPartialFill);
    }

    public static ThermalFluidStats create(int capacity, @NotNull FluidPipeProperties properties,
                                           boolean allowPartialFill) {
        return new ThermalFluidStats(capacity, properties.getMaxFluidTemperature(), properties.isGasProof(),
                properties.isAcidProof(), properties.isCryoProof(), properties.isPlasmaProof(), allowPartialFill);
    }

    public ResourceHandler<FluidResource> createHandler(ItemAccess access) {
        return new ThermalFluidResourceHandler(access, capacity, maxFluidTemperature, gasProof, acidProof, cryoProof,
                plasmaProof, allowPartialFill);
    }

    @Nullable
    public static ThermalFluidStats find(Item item) {
        if (item instanceof IComponentItem componentItem) {
            for (IItemComponent component : componentItem.getComponents()) {
                if (component instanceof ThermalFluidStats stats) {
                    return stats;
                }
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        var containedFluid = FluidUtil.getFluidContained(stack);
        if (containedFluid.isPresent()) {
            var tank = containedFluid.get();
            tooltipComponents
                    .add(Component.translatable("gtceu.universal.tooltip.fluid_stored", tank.getHoverName(),
                            tank.getAmount()));
            TooltipsHandler.appendFluidTooltips(tank, tooltipComponents::add, null);
        } else {
            tooltipComponents.add(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                    FormattingUtil.formatNumbers(capacity)));
        }
        if (GTUtil.isShiftDown()) {
            tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.max_temperature",
                    FormattingUtil.formatTemperature(maxFluidTemperature)));
            if (gasProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.gas_proof"));
            else tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.not_gas_proof"));
            if (plasmaProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.plasma_proof"));
            if (cryoProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.cryo_proof"));
            if (acidProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.acid_proof"));
        } else if (gasProof || cryoProof || plasmaProof || acidProof) {
            tooltipComponents.add(Component.translatable("gtceu.tooltip.fluid_pipe_hold_shift"));
        }
    }
}
