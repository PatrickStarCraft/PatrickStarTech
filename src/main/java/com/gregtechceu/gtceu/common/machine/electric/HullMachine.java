package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import org.jspecify.annotations.NullMarked;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.GuiTextures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NullMarked
public class HullMachine extends TieredPartMachine implements IMonitorComponent {

    @SaveField
    protected NotifiableEnergyContainer energyContainer;

    public HullMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        long tierVoltage = GTValues.V[getTier()];
        this.energyContainer = attachTrait(
                new NotifiableEnergyContainer(tierVoltage * 16L, tierVoltage, 1L, tierVoltage, 1L));
        this.energyContainer.setSideOutputCondition(s -> s == getFrontFacing());
    }

    @Override
    public int tintColor(int index) {
        if (index == 2) {
            return GTValues.VC[getTier()];
        }
        return super.tintColor(index);
    }

    @Override
    public IDrawable getIcon() {
        return GuiTextures.CROSS;
    }
}
