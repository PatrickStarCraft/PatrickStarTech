package com.gregtechceu.gtceu.common.data;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * Isolated-test holder for the production GT component's real NeoForge value type and codecs.
 * The full mod's deferred registry is not loaded by this selected-source test host.
 */
public final class GTDataComponents {

    public static final ComponentHolder<DataComponentType<SimpleFluidContent>> FLUID_CONTENT = new ComponentHolder<>(
            DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    private GTDataComponents() {}

    public record ComponentHolder<T>(T value) {
        public T get() {
            return value;
        }
    }
}
