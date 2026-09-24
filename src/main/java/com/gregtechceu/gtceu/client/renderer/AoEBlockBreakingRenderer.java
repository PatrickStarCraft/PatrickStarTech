package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.aoe.AoESymmetrical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import java.util.List;

/** Adds the active tool's AoE targets to vanilla's extracted block-breaking render states. */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class AoEBlockBreakingRenderer {

    @SubscribeEvent
    public static void addAoEBreakingStates(ExtractLevelRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player.isShiftKeyDown()) return;

        ItemStack mainHandItem = player.getMainHandItem();
        if (!ToolHelper.hasBehaviorsTag(mainHandItem) || !(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            return;
        }

        AoESymmetrical aoeDefinition = ToolHelper.getAoEDefinition(mainHandItem);
        if (aoeDefinition.isZero()) return;

        BlockPos hitPos = hitResult.getBlockPos();
        var level = event.getLevel();
        if (!mainHandItem.isCorrectToolForDrops(level.getBlockState(hitPos))) return;

        BlockBreakingRenderState hitProgress = findHitProgress(event, hitPos);
        if (hitProgress == null) return;

        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult);
        List<BlockPos> positions = ToolHelper.getHarvestableBlocks(aoeDefinition, context);
        for (BlockPos pos : positions) {
            event.getRenderState().blockBreakingRenderStates.add(new BlockBreakingRenderState(
                    pos.immutable(), level.getBlockState(pos), hitProgress.progress()));
        }
    }

    private static BlockBreakingRenderState findHitProgress(ExtractLevelRenderStateEvent event, BlockPos hitPos) {
        BlockBreakingRenderState found = null;
        for (BlockBreakingRenderState progress : event.getRenderState().blockBreakingRenderStates) {
            if (progress.blockPos().equals(hitPos)) {
                found = progress;
            }
        }
        return found;
    }

    private AoEBlockBreakingRenderer() {}
}
