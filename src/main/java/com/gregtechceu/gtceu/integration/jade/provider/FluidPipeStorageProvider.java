package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.*;

import java.util.ArrayList;
import java.util.List;

public enum FluidPipeStorageProvider implements IServerExtensionProvider<FluidView.Data>,
        IClientExtensionProvider<FluidView.Data, FluidView> {

    INSTANCE;

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, (group, clientGroup) -> {
            if (group.id != null) {
                clientGroup.title = Component.literal(group.id);
            }
        });
    }

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof FluidPipeBlockEntity pipe)) return null;
        List<FluidView.Data> fluids = new ArrayList<>();
        for (var tank : pipe.getFluidTanks()) {
            if (tank.getFluidAmount() > 0) {
                var fluid = tank.getFluid();
                fluids.add(new FluidView.Data(
                        JadeFluidObject.of(fluid.getFluid(), tank.getFluidAmount(), fluid.getComponentsPatch()),
                        tank.getCapacity()));
            }
        }
        return fluids.isEmpty() ? List.of() : List.of(new ViewGroup<>(fluids));
    }

    @Override
    public Identifier getUid() {
        return GTCEu.id("fluid_storage");
    }
}
