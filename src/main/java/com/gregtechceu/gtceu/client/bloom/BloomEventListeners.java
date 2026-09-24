package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import lombok.experimental.UtilityClass;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@UtilityClass
public class BloomEventListeners {

    @SubscribeEvent
    public static void afterLevelRendered(RenderLevelStageEvent.AfterLevel event) {
        Minecraft minecraft = Minecraft.getInstance();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        BloomRenderer.renderBloom(minecraft.gameRenderer.mainCamera(), event.getPoseStack(), camera.cullFrustum,
                camera.projectionMatrix, partialTick, event.getLevelRenderer(), Profiler.get());
    }

    @SubscribeEvent
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        if (Minecraft.getInstance().level == null) return;
        if (!BloomShaderManager.isBloomActive()) return;

        var bloomColor = BloomShaderManager.BLOOM_TARGET.getColorTexture();
        if (bloomColor != null) {
            var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
            encoder.clearColorTexture(bloomColor, new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
            var depth = BloomShaderManager.BLOOM_TARGET.getDepthTexture();
            if (depth != null) encoder.clearDepthTexture(depth, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        BloomShaderManager.updateShaderAvailability(event);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        BloomHandler.invalidateLevelData(event.getLevel());

        if (BloomRenderer.SafeMode.enabled()) {
            BloomRenderer.SafeMode.invalidateLevelData();
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!BloomShaderManager.isBloomActive()) return;

        ChunkAccess chunk = event.getChunk();
        LevelAccessor level = event.getLevel();

        if (!BloomRenderer.SafeMode.enabled()) return;

        ChunkPos chunkPos = chunk.getPos();
        int minSection = level.getMinSectionY(), maxSection = level.getMaxSectionY();
        for (int y = minSection; y <= maxSection; y++) {
            BloomRenderer.SafeMode.invalidateSectionData(SectionPos.of(chunkPos.x(), y, chunkPos.z()));
        }
    }

    // Merge into parent class in 1.21, event listener discovery is smarter there
    @net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
    @UtilityClass
    public static class ModBus {

        @SubscribeEvent
        public static void registerNamedRenderTypes(RegisterNamedRenderTypesEvent event) {
            RenderType block, entity;
            if (!BloomRenderer.SafeMode.enabled() && BloomShaderManager.isBloomAvailable()) {
                block = GTRenderTypes.bloom();
                entity = GTRenderTypes.entityBloomBlockSheet();
            } else {
                // if safe mode is enabled, register the named render type as a copy of forge's 'cutout'
                block = RenderType.cutoutMipped();
                entity = ForgeRenderTypes.ITEM_LAYERED_CUTOUT.get();
            }
            event.register("bloom", block, entity);
        }
    }
}
