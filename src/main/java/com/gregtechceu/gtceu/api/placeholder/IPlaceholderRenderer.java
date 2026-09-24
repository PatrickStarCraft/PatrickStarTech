package com.gregtechceu.gtceu.api.placeholder;

import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.nbt.CompoundTag;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;

public interface IPlaceholderRenderer {

    MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group, float partialTick,
                                             CompoundTag tag);
}
