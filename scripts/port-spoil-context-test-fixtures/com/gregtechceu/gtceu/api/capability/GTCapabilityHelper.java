package com.gregtechceu.gtceu.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/** Compile-only adapter for the unused live block-capability lookup branch. */
public final class GTCapabilityHelper {

    private GTCapabilityHelper() {}

    public static @Nullable IItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        return null;
    }

    public static @Nullable ISpoilableItem getSpoilable(ItemStack stack) {
        return null;
    }
}
