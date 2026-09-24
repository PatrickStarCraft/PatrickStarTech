package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.Direction;

public interface IDynamicCoverRenderer {

    DynamicCoverRenderSnapshot extractRenderState(MetaMachine machine, Direction face, float partialTick);
}
