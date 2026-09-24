package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.steam.SteamMachine;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class ColoredAoEBlockOutlineRenderer {

    @SubscribeEvent
    public static void addAoEOutlines(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        BlockState hitState = event.getBlockState();
        if (player == null || player.isShiftKeyDown() || hitState.isAir() ||
                !event.getLevel().isInWorldBounds(event.getBlockPos())) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        if (!mainHandItem.isCorrectToolForDrops(hitState) || !ToolHelper.hasBehaviorsTag(mainHandItem)) return;

        BlockHitResult hitResult = event.getHitResult();
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult);
        List<BlockPos> positions = ToolHelper.getHarvestableBlocks(ToolHelper.getAoEDefinition(mainHandItem), context);
        positions.sort((first, second) -> {
            boolean firstMaterial = event.getLevel().getBlockState(first).getBlock() instanceof MaterialBlock;
            boolean secondMaterial = event.getLevel().getBlockState(second).getBlock() instanceof MaterialBlock;
            return firstMaterial == secondMaterial ? 0 : firstMaterial ? 1 : -1;
        });

        List<AoeOutline> outlines = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockState state = event.getLevel().getBlockState(pos);
            int color = resolveColor(event.getLevel(), pos, state);
            VoxelShape shape = state.getShape(event.getLevel(), pos, event.getCollisionContext());
            outlines.add(new AoeOutline(pos.immutable(), shape, color));
        }

        Vec3 cameraPos = event.getCamera().position();
        double cameraX = cameraPos.x();
        double cameraY = cameraPos.y();
        double cameraZ = cameraPos.z();
        boolean highContrast = event.isHighContrast();
        boolean afterTerrain = event.isInTranslucentPass();
        float lineWidth = minecraft.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        List<AoeOutline> snapshot = List.copyOf(outlines);

        event.addCustomRenderer((renderState, collector, poseStack, levelRenderState) -> {
            for (AoeOutline outline : snapshot) {
                poseStack.pushPose();
                try {
                    BlockPos pos = outline.pos();
                    poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                    if (highContrast) {
                        collector.submitShapeOutline(poseStack, outline.shape(), RenderTypes.secondaryBlockOutline(),
                                0xFF000000, 7.0F, afterTerrain);
                    }
                    int defaultColor = highContrast ? -11010079 : ARGB.black(102);
                    collector.submitShapeOutline(poseStack, outline.shape(), RenderTypes.lines(),
                            outline.color() != 0 ? outline.color() : defaultColor, lineWidth, afterTerrain);
                } finally {
                    poseStack.popPose();
                }
            }
            return true;
        });
    }

    private static int resolveColor(net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos, BlockState state) {
        var rendererConfig = ConfigHolder.INSTANCE.client.renderer;
        MaterialEntry materialEntry = ChemicalHelper.getMaterialEntry(state.getBlock());
        if (rendererConfig.coloredMaterialBlockOutline && materialEntry != null) {
            return withOutlineAlpha(materialEntry.material().getMaterialRGB());
        } else if (rendererConfig.coloredTieredMachineOutline) {
            if (level.getBlockEntity(pos) instanceof SteamMachine steam) {
                return withOutlineAlpha(steam.isHighPressure() ? GTValues.VC_HP_STEAM : GTValues.VC_LP_STEAM);
            }
            if (level.getBlockEntity(pos) instanceof ITieredMachine tiered) {
                return withOutlineAlpha(GTValues.VCM[tiered.getTier()]);
            }
        } else if (rendererConfig.coloredWireOutline && level.getBlockEntity(pos) instanceof IPipeNode<?, ?> pipe) {
            if (pipe.getFrameMaterial() != null) {
                return withOutlineAlpha(pipe.getFrameMaterial().getMaterialRGB());
            }
            if (pipe instanceof CableBlockEntity cable) {
                return withOutlineAlpha(GTValues.VCM[GTUtil.getTierByVoltage(cable.getNodeData().getVoltage())]);
            }
            if (state.getBlock() instanceof MaterialPipeBlock<?, ?, ?> materialPipe) {
                return withOutlineAlpha(materialPipe.material.getMaterialRGB());
            }
        }
        return 0;
    }

    private static int withOutlineAlpha(int rgb) {
        return ARGB.color(102, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb));
    }

    private record AoeOutline(BlockPos pos, VoxelShape shape, int color) {}

    private ColoredAoEBlockOutlineRenderer() {}
}
