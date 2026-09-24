package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.BlockAttributes;
import com.gregtechceu.gtceu.client.renderer.PatternPreviewRenderer;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.gregtechceu.gtceu.common.commands.GTClientCommands;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;


@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ClientEventListener {

    @SubscribeEvent
    public static void updateFOV(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null || moveSpeed.getModifier(BlockAttributes.BLOCK_SPEED_BOOST) == null) return;

        float multi = 1;
        var state = player.level().getBlockState(player.getOnPos());

        if (state.is(CustomTags.VERY_FAST_WALKABLE_BLOCKS)) multi /= 1.2F;

        multi = (float) Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get(), 1.0F, multi);
        event.setNewFovModifier(event.getNewFovModifier() * multi);
    }

    private static double getValueWithoutWalkingBoost(AttributeInstance attrib) {
        double base = attrib.getBaseValue();

        for (AttributeModifier mod : attrib.getModifiers()) {
            if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) {
                base += mod.amount();
            }
        }

        double applied = base;
        for (AttributeModifier mod : attrib.getModifiers()) {
            if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE &&
                    !mod.id().equals(BlockAttributes.BLOCK_SPEED_BOOST)) {
                applied += base * mod.amount();
            }
        }

        for (AttributeModifier mod : attrib.getModifiers()) {
            if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                applied *= 1 + mod.amount();
            }
        }

        return attrib.getAttribute().sanitizeValue(applied);
    }

    @SubscribeEvent
    public static void onTooltipEvent(ItemTooltipEvent event) {
        TooltipsHandler.appendTooltips(event.getItemStack(), event.getFlags(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onClientTickEvent(ClientTickEvent.Post event) {
        TooltipHelper.onClientTick();
        EnvironmentalHazardClientHandler.INSTANCE.onClientTick();
        PatternPreviewRenderer.INSTANCE.clientTick();

        GTValues.CLIENT_TIME++;
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCacheManager.allowReinit();
    }

    @SubscribeEvent
    public static void registerClientCommand(RegisterClientCommandsEvent event) {
        GTClientCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ClientCacheManager.clearCaches();
    }
}
