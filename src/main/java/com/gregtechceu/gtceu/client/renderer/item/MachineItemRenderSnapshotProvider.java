package com.gregtechceu.gtceu.client.renderer.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item-specific snapshot hook for dynamic content on machine block items. */
public interface MachineItemRenderSnapshotProvider {

    @Nullable MachineItemRenderSnapshot extractItemRenderState(ItemStack stack);
}
