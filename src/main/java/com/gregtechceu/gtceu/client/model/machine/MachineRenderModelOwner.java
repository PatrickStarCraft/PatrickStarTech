package com.gregtechceu.gtceu.client.model.machine;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

/** A baked machine model that can provide its definition to dynamic render descriptors. */
public interface MachineRenderModelOwner {

    MachineDefinition getDefinition();
}
