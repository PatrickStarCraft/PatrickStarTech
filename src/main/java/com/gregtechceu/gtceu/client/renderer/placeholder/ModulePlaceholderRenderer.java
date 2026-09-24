package com.gregtechceu.gtceu.client.renderer.placeholder;

import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.placeholder.IPlaceholderRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorRenderSnapshot;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;


import java.util.ArrayList;
import java.util.List;

public class ModulePlaceholderRenderer implements IPlaceholderRenderer {

    @Override
    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick, CompoundTag tag) {
        ItemStack stack = com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(tag);
        if (stack.getItem() instanceof IComponentItem componentItem) {
            List<MonitorRenderSnapshot> snapshots = new ArrayList<>();
            for (IItemComponent component : componentItem.getComponents()) {
                if (component instanceof IMonitorModuleItem module) {
                    IMonitorRenderer renderer = module.getRenderer(stack, machine, group);
                    if (renderer == null) continue;
                    MonitorRenderSnapshot snapshot = renderer.extractRenderState(machine, group, partialTick);
                    if (snapshot != null) snapshots.add(snapshot);
                }
            }
            if (!snapshots.isEmpty()) return new ModuleSnapshot(snapshots);
        }
        return null;
    }

    private record ModuleSnapshot(List<MonitorRenderSnapshot> modules) implements MonitorRenderSnapshot {
        private ModuleSnapshot { modules = List.copyOf(modules); }

        @Override
        public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.SubmitNodeCollector collector) {
            for (MonitorRenderSnapshot module : this.modules) module.submit(poseStack, collector);
        }
    }
}
