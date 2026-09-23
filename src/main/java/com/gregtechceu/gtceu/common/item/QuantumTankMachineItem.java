package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.common.machine.storage.QuantumTankMachine;

public class QuantumTankMachineItem extends MetaMachineItem {

    public QuantumTankMachineItem(MetaMachineBlock block, Properties properties) {
        super(block, properties);
    }

    public long getFluidCapacity() {
        if (!QuantumTankMachine.TANK_CAPACITY.containsKey(getDefinition())) {
            GTCEu.LOGGER
                    .error("Quantum tank " + getDefinition().getName() + " does not have a registered TANK_CAPACITY," +
                            " will have capacity 0.");
        }
        return QuantumTankMachine.TANK_CAPACITY.getLong(getDefinition());
    }
}
