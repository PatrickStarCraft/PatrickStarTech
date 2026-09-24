package com.gregtechceu.gtceu.api.item.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Compile-only tool service. The counter observes calls from the selected transaction handler;
 * actual GT durability rules are not represented by this fixture.
 */
public final class ToolHelper {

    private static int damageCalls;

    private ToolHelper() {}

    public static void onActionDone(Player player, ItemStack stack, Level level, Vec3 location) {}

    public static void damageItem(ItemStack stack, Player player) {
        damageCalls++;
    }

    public static void resetDamageCalls() {
        damageCalls = 0;
    }

    public static int damageCalls() {
        return damageCalls;
    }
}
