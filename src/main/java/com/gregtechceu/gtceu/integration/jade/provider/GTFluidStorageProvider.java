package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.storage.CreativeTankMachine;
import com.gregtechceu.gtceu.common.machine.storage.QuantumTankMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferProxyPartMachine;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom FluidView info provider for any machines that require it
 * Currently: Quantum Tanks, Pattern Buffer Proxies
 * Defaults to Jade's normal FluidView provider
 */
public enum GTFluidStorageProvider implements IServerExtensionProvider<FluidView.Data>,
        IClientExtensionProvider<FluidView.Data, FluidView> {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return GTCEu.id("custom_fluid_storage");
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof MetaMachine machine)) return null;
        if (machine instanceof QuantumTankMachine qtm) {
            FluidStack stored = qtm.getStored();
            if (stored.isEmpty() && qtm instanceof CreativeTankMachine) return Collections.emptyList();
            if (stored.isEmpty() && qtm.isLocked()) stored = qtm.getLockedFluid();
            long amount = qtm.getStoredAmount();
            return List.of(new ViewGroup<>(List.of(new FluidView.Data(
                    JadeFluidObject.of(stored.getFluid(), amount, stored.getComponentsPatch()),
                    qtm.getMaxAmount()))));
        } else if (GTCEu.Mods.isAE2Loaded() && machine instanceof MEPatternBufferPartMachine buffer) {
            var tank = buffer.getShareTank();
            List<FluidView.Data> list = new ArrayList<>(tank.getTanks());
            for (var storage : tank.getStorages()) {
                var stack = storage.getFluid();
                if (stack.isEmpty()) continue;
                int capacity = storage.getCapacity();
                list.add(new FluidView.Data(JadeFluidObject.of(stack.getFluid(), stack.getAmount(),
                        stack.getComponentsPatch()), capacity));
            }
            return list.isEmpty() ? List.of() : List.of(new ViewGroup<>(list));
        } else if (GTCEu.Mods.isAE2Loaded() && machine instanceof MEPatternBufferProxyPartMachine proxy) {
            var buffer = proxy.getBuffer();
            if (buffer == null) return Collections.emptyList();
            return FluidStorageProvider.Extension.INSTANCE.getGroups(accessor);
        }

        return FluidStorageProvider.Extension.INSTANCE.getGroups(accessor);
    }
}
