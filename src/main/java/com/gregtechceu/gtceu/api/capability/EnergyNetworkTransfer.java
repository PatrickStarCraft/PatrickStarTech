package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Distributes a single packet budget without offering already accepted amperes to another receiver. */
public final class EnergyNetworkTransfer {

    private EnergyNetworkTransfer() {}

    public static long distribute(List<? extends IEnergyContainer> containers, @Nullable Direction side,
                                  long voltage, long amperage) {
        if (voltage <= 0 || amperage <= 0) return 0;
        long accepted = 0;
        for (var container : containers) {
            accepted += container.acceptEnergyFromNetwork(side, voltage, amperage - accepted);
            if (accepted >= amperage) break;
        }
        return accepted;
    }
}
