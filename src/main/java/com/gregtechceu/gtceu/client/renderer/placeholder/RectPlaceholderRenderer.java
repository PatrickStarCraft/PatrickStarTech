package com.gregtechceu.gtceu.client.renderer.placeholder;

import com.gregtechceu.gtceu.api.placeholder.IPlaceholderRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorQuadSnapshot;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.nbt.CompoundTag;

public class RectPlaceholderRenderer implements IPlaceholderRenderer {

    @Override
    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick, CompoundTag tag) {
        float minX = 0, maxX = tag.getFloatOr("width", 0.0F);
        float minY = 0, maxY = tag.getFloatOr("height", 0.0F);
        int color = tag.getIntOr("color", 0);
        return new MonitorQuadSnapshot(new float[] { minX, maxY, maxX, maxY, maxX, minY, minX, minY },
                new int[] { color, color, color, color });
    }
}
