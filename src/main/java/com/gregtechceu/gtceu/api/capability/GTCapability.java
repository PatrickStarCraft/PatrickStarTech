package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.common.capability.MedicalConditionTracker;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

/** NeoForge capabilities are registered against their block, item or entity providers, not interface classes. */
public final class GTCapability {
    public static final BlockCapability<IEnergyContainer, Direction> CAPABILITY_ENERGY_CONTAINER =
            BlockEnergyCapabilities.ENERGY_CONTAINER;
    public static final BlockCapability<IEnergyInfoProvider, Direction> CAPABILITY_ENERGY_INFO_PROVIDER =
            block("energy_info_provider", IEnergyInfoProvider.class);
    public static final BlockCapability<ICoverable, Direction> CAPABILITY_COVERABLE = block("coverable", ICoverable.class);
    public static final BlockCapability<IWorkable, Direction> CAPABILITY_WORKABLE = block("workable", IWorkable.class);
    public static final BlockCapability<IControllable, Direction> CAPABILITY_CONTROLLABLE = block("controllable", IControllable.class);
    public static final ItemCapability<IElectricItem, Void> CAPABILITY_ELECTRIC_ITEM = ElectricItemCapabilities.ELECTRIC_ITEM;
    public static final BlockCapability<ILaserContainer, Direction> CAPABILITY_LASER = block("laser", ILaserContainer.class);
    public static final BlockCapability<IOpticalComputationProvider, Direction> CAPABILITY_COMPUTATION_PROVIDER =
            block("computation_provider", IOpticalComputationProvider.class);
    public static final BlockCapability<IDataAccessHatch, Direction> CAPABILITY_DATA_ACCESS = block("data_access", IDataAccessHatch.class);
    public static final BlockCapability<IHazardParticleContainer, Direction> CAPABILITY_HAZARD_CONTAINER =
            block("hazard_container", IHazardParticleContainer.class);
    public static final BlockCapability<IMonitorComponent, Direction> CAPABILITY_MONITOR_COMPONENT =
            block("monitor_component", IMonitorComponent.class);
    public static final ItemCapability<ISpoilableItem, Void> CAPABILITY_SPOILABLE_ITEM =
            ItemCapability.createVoid(Identifier.fromNamespaceAndPath("gtceu", "spoilable_item"), ISpoilableItem.class);
    public static final EntityCapability<MedicalConditionTracker, Void> CAPABILITY_MEDICAL_CONDITION_TRACKER =
            EntityCapability.createVoid(Identifier.fromNamespaceAndPath("gtceu", "medical_condition_tracker"), MedicalConditionTracker.class);

    private static <T> BlockCapability<T, Direction> block(String name, Class<T> type) {
        return BlockCapability.createSided(Identifier.fromNamespaceAndPath("gtceu", name), type);
    }
}
