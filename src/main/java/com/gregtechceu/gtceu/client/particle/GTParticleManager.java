package com.gregtechceu.gtceu.client.particle;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.bloom.BloomShaderManager;
import com.gregtechceu.gtceu.client.bloom.EffectRenderContext;
import com.gregtechceu.gtceu.client.bloom.particle.GTBloomParticle;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Singleton class responsible for managing, updating and rendering {@link GTParticle} instances.
 */
public final class GTParticleManager {

    public static final GTParticleManager INSTANCE = new GTParticleManager();

    private final Map<@Nullable RenderType, Queue<GTParticle>> depthEnabledParticles = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<@Nullable RenderType, Queue<GTParticle>> depthDisabledParticles = new Object2ObjectLinkedOpenHashMap<>();

    private final List<GTParticle> newParticleQueue = new ArrayList<>();

    private GTParticleManager() {}

    public void addEffect(GTParticle particles) {
        newParticleQueue.add(particles);
    }

    public void updateEffects() {
        if (!this.depthEnabledParticles.isEmpty()) {
            updateQueue(this.depthEnabledParticles);
        }
        if (!this.depthDisabledParticles.isEmpty()) {
            updateQueue(this.depthDisabledParticles);
        }

        if (!newParticleQueue.isEmpty()) {
            for (GTParticle particle : newParticleQueue) {
                var queue = particle.shouldDisableDepth() ? this.depthDisabledParticles : this.depthEnabledParticles;

                Queue<GTParticle> particles = queue.computeIfAbsent(
                        particle.getRenderType(!particle.shouldDisableDepth()),
                        setup -> new ArrayDeque<>());

                if (particles.size() > 6000) {
                    particles.remove().setExpired();
                }
                particles.add(particle);
            }

            newParticleQueue.clear();
        }
    }

    private void updateQueue(Map<@Nullable RenderType, Queue<GTParticle>> renderQueue) {
        Iterator<Queue<GTParticle>> it = renderQueue.values().iterator();
        while (it.hasNext()) {
            Queue<GTParticle> particlesForSetup = it.next();

            Iterator<GTParticle> particles = particlesForSetup.iterator();
            while (particles.hasNext()) {
                GTParticle particle = particles.next();

                if (particle.isAlive()) {
                    try {
                        particle.onUpdate();
                    } catch (RuntimeException exception) {
                        GTCEu.LOGGER.error("Particle update error: {}", particle, exception);
                        particle.setExpired();
                    }
                    if (particle.isAlive()) continue;
                }

                particles.remove();
            }

            if (particlesForSetup.isEmpty()) {
                it.remove();
            }
        }
    }

    public void clearAllEffects(boolean cleanNewQueue) {
        if (cleanNewQueue) {
            for (GTParticle particle : this.newParticleQueue) {
                particle.setExpired();
            }
            this.newParticleQueue.clear();
        }
        for (Queue<GTParticle> particles : this.depthEnabledParticles.values()) {
            for (GTParticle particle : particles) {
                particle.setExpired();
            }
        }
        for (Queue<GTParticle> particles : this.depthDisabledParticles.values()) {
            for (GTParticle particle : particles) {
                particle.setExpired();
            }
        }
        this.depthEnabledParticles.clear();
        this.depthDisabledParticles.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void renderParticles(SubmitCustomGeometryEvent event) {
        if (this.depthEnabledParticles.isEmpty() && this.depthDisabledParticles.isEmpty()) return;

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Entity cameraEntity = minecraft.getCameraEntity();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        EffectRenderContext instance = EffectRenderContext.getInstance()
                .update(camera, camera.cullFrustum, partialTick, cameraEntity);

        if (!this.depthDisabledParticles.isEmpty()) {
            renderParticlesInLayer(event.getSubmitNodeCollector(), poseStack, this.depthDisabledParticles, instance);
        }
        renderParticlesInLayer(event.getSubmitNodeCollector(), poseStack, this.depthEnabledParticles, instance);

        poseStack.popPose();
    }

    private static void renderParticlesInLayer(SubmitNodeCollector collector, PoseStack poseStack,
                                               Map<@Nullable RenderType, Queue<GTParticle>> renderQueue,
                                               EffectRenderContext context) {
        for (var entry : renderQueue.entrySet()) {
            Queue<GTParticle> particles = entry.getValue();
            RenderType renderType = entry.getKey();
            if (particles.isEmpty() || renderType == null) continue;
            for (GTParticle particle : particles) {
                if (!particle.shouldRender(context)) {
                    continue;
                }
                try {
                    particle.renderParticle(collector, poseStack, renderType, context);
                    if (BloomShaderManager.isBloomActive() && particle instanceof GTBloomParticle bloomParticle) {
                        bloomParticle.renderBloomParticle(collector, poseStack, context);
                    }

                } catch (Throwable throwable) {
                    GTCEu.LOGGER.error("Particle render error: {}", particle, throwable);
                    particle.setExpired();
                }
            }
        }
    }

    @SubscribeEvent
    public void clientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        if (Minecraft.getInstance().level != null) {
            INSTANCE.updateEffects();
        }
    }

    @SubscribeEvent
    public void onClientLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel newLevel)) {
            return;
        }
        ClientLevel oldLevel = Minecraft.getInstance().level;
        if (oldLevel != newLevel) {
            this.clearAllEffects(oldLevel != null);
        }
    }

    @SubscribeEvent
    public void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (event.getPlayer() != null) {
            this.clearAllEffects(true);
        }
    }

    public void registerDebugEntries(RegisterDebugEntriesEvent event) {
        Identifier entryId = Identifier.fromNamespaceAndPath("gtceu", "particle_counts");
        event.register(entryId, (DebugScreenDisplayer displayer, @Nullable Level level,
                                 @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) ->
                displayer.addLine(ChatFormatting.GOLD + "P-BACK: " + count(this.depthEnabledParticles) +
                        " P-FRONT: " + count(this.depthDisabledParticles)));
        event.includeInProfile(entryId, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.ALWAYS_ON);
    }

    private static int count(Map<@Nullable RenderType, Queue<GTParticle>> renderQueue) {
        return renderQueue.values().stream().mapToInt(Queue::size).sum();
    }
}
