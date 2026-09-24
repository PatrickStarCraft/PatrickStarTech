package com.gregtechceu.gtceu.client.renderer.placeholder;

import com.gregtechceu.gtceu.api.placeholder.IPlaceholderRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorQuadSnapshot;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.nbt.CompoundTag;

public class QuadPlaceholderRenderer implements IPlaceholderRenderer {

    @Override
    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick, CompoundTag tag) {
        return new MonitorQuadSnapshot(new float[] {
                tag.getFloat("x1").orElse(0.0F), tag.getFloat("y1").orElse(0.0F),
                tag.getFloat("x2").orElse(0.0F), tag.getFloat("y2").orElse(0.0F),
                tag.getFloat("x3").orElse(0.0F), tag.getFloat("y3").orElse(0.0F),
                tag.getFloat("x4").orElse(0.0F), tag.getFloat("y4").orElse(0.0F) },
                new int[] { tag.getInt("color1").orElse(0), tag.getInt("color2").orElse(0),
                        tag.getInt("color3").orElse(0), tag.getInt("color4").orElse(0) });
    }
}
