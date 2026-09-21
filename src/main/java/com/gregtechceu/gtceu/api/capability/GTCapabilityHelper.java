package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.common.capability.MedicalConditionTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("DataFlowIssue")
public class GTCapabilityHelper {

    @Nullable
    public static IElectricItem getElectricItem(ItemStack itemStack) {
        return itemStack.getCapability(ElectricItemCapabilities.ELECTRIC_ITEM);
    }

    @Nullable
    public static IEnergyStorage getForgeEnergyItem(ItemStack itemStack) {
        var handler = itemStack.getCapability(Capabilities.Energy.ITEM,
                net.neoforged.neoforge.transfer.access.ItemAccess.forStack(itemStack));
        return handler == null ? null : IEnergyStorage.of(handler);
    }

    @Nullable
    public static IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        var gtHandler = level.getCapability(GTTransferCapabilities.ITEM, pos, side);
        if (gtHandler != null) return gtHandler;
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler == null ? null : IItemHandler.of(handler);
    }

    @Nullable
    public static IFluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        var gtHandler = level.getCapability(GTTransferCapabilities.FLUID, pos, side);
        if (gtHandler != null) return gtHandler;
        var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler == null ? null : IFluidHandler.of(handler);
    }

    @Nullable
    public static IEnergyContainer getEnergyContainer(Level level, BlockPos pos, @Nullable Direction side) {
        return level.getCapability(BlockEnergyCapabilities.ENERGY_CONTAINER, pos, side);
    }

    @Nullable
    public static IEnergyInfoProvider getEnergyInfoProvider(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_ENERGY_INFO_PROVIDER, level, pos, side);
    }

    @Nullable
    public static ICoverable getCoverable(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_COVERABLE, level, pos, side);
    }

    @Nullable
    public static IWorkable getWorkable(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_WORKABLE, level, pos, side);
    }

    @Nullable
    public static IControllable getControllable(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_CONTROLLABLE, level, pos, side);
    }

    @Nullable
    public static IEnergyStorage getForgeEnergy(Level level, BlockPos pos, @Nullable Direction side) {
        var gtHandler = level.getCapability(GTTransferCapabilities.ENERGY, pos, side);
        if (gtHandler != null) return gtHandler;
        var handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler == null ? null : IEnergyStorage.of(handler);
    }

    @Nullable
    public static ILaserContainer getLaser(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_LASER, level, pos, side);
    }

    @Nullable
    public static IOpticalComputationProvider getOpticalComputationProvider(Level level, BlockPos pos,
                                                                            @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_COMPUTATION_PROVIDER, level, pos, side);
    }

    @Nullable
    public static IDataAccessHatch getDataAccess(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_DATA_ACCESS, level, pos, side);
    }

    @Nullable
    public static IHazardParticleContainer getHazardContainer(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_HAZARD_CONTAINER, level, pos, side);
    }

    @Nullable
    public static IMonitorComponent getMonitorComponent(Level level, BlockPos pos, @Nullable Direction side) {
        return getBlockEntityCapability(GTCapability.CAPABILITY_MONITOR_COMPONENT, level, pos, side);
    }

    @Nullable
    public static <T> T getBlockEntityCapability(BlockCapability<T, Direction> capability, Level level, BlockPos pos,
                                                  @Nullable Direction side) {
        return level.getCapability(capability, pos, side);
    }

    public static <T> @Nullable T getBlockEntityCapability(BlockEntity entity,
                                                          BlockCapability<T, Direction> capability,
                                                          @Nullable Direction side) {
        var level = entity.getLevel();
        return level == null || entity.isRemoved() ? null :
                level.getCapability(capability, entity.getBlockPos(), entity.getBlockState(), entity, side);
    }

    @Nullable
    public static MedicalConditionTracker getMedicalConditionTracker(@NotNull Entity entity) {
        return entity.getCapability(GTCapability.CAPABILITY_MEDICAL_CONDITION_TRACKER);
    }

    @Nullable
    public static ISpoilableItem getSpoilable(ItemStack stack) {
        return stack.getCapability(GTCapability.CAPABILITY_SPOILABLE_ITEM);
    }
}
