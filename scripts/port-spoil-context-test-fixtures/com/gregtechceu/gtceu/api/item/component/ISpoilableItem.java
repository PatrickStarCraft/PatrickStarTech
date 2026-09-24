package com.gregtechceu.gtceu.api.item.component;

import net.minecraft.world.item.ItemStack;

/** Compile-only capability contract; the selected suite does not load live capabilities. */
public interface ISpoilableItem {

    void updateFreshness(SpoilContext context, boolean createTag);

    long getSpoilTicks();

    long getTicksUntilSpoiled();

    void setTicksUntilSpoiled(long value);

    void freezeSpoiling();

    void unfreezeSpoiling();

    boolean isFrozen();

    ItemStack spoilResult(SpoilContext context, boolean simulate);

    boolean shouldSpoil();

    long getCreationTick();

    void setCreationTick(long tick);
}
