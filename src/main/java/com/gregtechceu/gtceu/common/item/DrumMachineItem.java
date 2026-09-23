package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidPipeProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.item.component.ThermalFluidStats;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DrumMachineItem extends MetaMachineItem {

    private final Material mat;

    protected DrumMachineItem(MetaMachineBlock block, Properties properties, Material mat) {
        super(block, properties);
        this.mat = Objects.requireNonNull(mat, "Drum Material cannot be null");
    }

    public static DrumMachineItem create(MetaMachineBlock block, Properties properties, Material mat) {
        return new DrumMachineItem(block, properties, mat);
    }

    @Nullable
    public ThermalFluidStats getThermalFluidStats() {
        final FluidPipeProperties property = mat.getProperty(PropertyKey.FLUID_PIPE);
        if (property == null) {
            return null;
        }
        return ThermalFluidStats.create(GTMachineUtils.DRUM_CAPACITY.getInt(getDefinition()), property, false);
    }
}
