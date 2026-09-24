package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

import lombok.experimental.UtilityClass;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@UtilityClass
public class BloomEventListeners {

    private static boolean bloomWasActive;

    @SubscribeEvent
    public static void afterLevelRendered(RenderLevelStageEvent.AfterLevel event) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        BloomRenderer.renderBloom(partialTick, Profiler.get());
    }

    @SubscribeEvent
    public static void addStaticBloomGeometry(AddSectionGeometryEvent event) {
        BloomChunkGeometry.addSectionRenderer(event);
    }

    @SubscribeEvent
    public static void submitStaticBloomGeometry(SubmitCustomGeometryEvent event) {
        BloomChunkGeometry.submitVisible(event);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(GTCEu.id("bloom_chunk_geometry"), (ResourceManagerReloadListener)
                resourceManager -> BloomRenderer.SafeMode.invalidateLevelData());
    }

    @SubscribeEvent
    public static void onClientResourcesLoaded(ClientResourceLoadFinishedEvent event) {
        BloomShaderManager.initPostShaders();
        bloomWasActive = BloomShaderManager.isBloomActive();
        if (!event.isInitial() && bloomWasActive) {
            rebuildBloomSections();
        }
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
        boolean bloomIsActive = BloomShaderManager.isBloomActive();
        if (bloomIsActive == bloomWasActive) return;
        bloomWasActive = bloomIsActive;

        BloomRenderer.SafeMode.invalidateLevelData();
        if (bloomIsActive) {
            rebuildBloomSections();
        }
    }

    private static void rebuildBloomSections() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        minecraft.levelRenderer.invalidateCompiledGeometry(minecraft.level, minecraft.options,
                minecraft.gameRenderer.mainCamera(), minecraft.getBlockColors());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel)) return;
        ClientLevel currentLevel = Minecraft.getInstance().level;
        if (currentLevel != null && event.getLevel() != currentLevel) return;
        BloomHandler.invalidateLevelData(event.getLevel());
        BloomRenderer.SafeMode.invalidateLevelData();
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() != Minecraft.getInstance().level) return;
        ChunkAccess chunk = event.getChunk();
        LevelAccessor level = event.getLevel();
        ChunkPos chunkPos = chunk.getPos();
        int minSection = level.getMinSectionY(), maxSection = level.getMaxSectionY();
        for (int y = minSection; y <= maxSection; y++) {
            BloomRenderer.SafeMode.invalidateSectionData(SectionPos.of(chunkPos.x(), y, chunkPos.z()));
        }
    }
}
