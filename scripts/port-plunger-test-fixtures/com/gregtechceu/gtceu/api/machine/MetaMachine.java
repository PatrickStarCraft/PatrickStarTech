package com.gregtechceu.gtceu.api.machine;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Compile-only machine view for PlungerBehavior's untested machine lookup path. */
public interface MetaMachine {

    IFluidHandler getFluidHandlerCap(Direction side, boolean allowNull);
}
