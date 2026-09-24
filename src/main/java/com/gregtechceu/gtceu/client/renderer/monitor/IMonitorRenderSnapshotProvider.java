package com.gregtechceu.gtceu.client.renderer.monitor;

import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-owned extraction contract for monitor modules using the deferred render pipeline. */
@OnlyIn(Dist.CLIENT)
public interface IMonitorRenderSnapshotProvider {

    MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group, float partialTick);
}
