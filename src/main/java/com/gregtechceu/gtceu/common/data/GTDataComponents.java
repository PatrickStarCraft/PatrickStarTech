package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Persistent fluid content for GT item containers backed by NeoForge item access. */
public final class GTDataComponents {

    private static final DeferredRegister.DataComponents TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GTCEu.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            TYPES.registerComponentType("fluid_content", builder -> builder
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private GTDataComponents() {}

    public static void init(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
