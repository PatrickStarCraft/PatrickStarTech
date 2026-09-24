package com.gregtechceu.gtceu.client.model.machine;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/** Immutable output-face state consumed by machine block geometry on meshing workers. */
public record MachineOutputRenderState(@Nullable Direction itemOutputDirection, boolean autoOutputItems,
                                       @Nullable Direction fluidOutputDirection, boolean autoOutputFluids) {

    public static final MachineOutputRenderState EMPTY = new MachineOutputRenderState(null, false, null, false);
}
