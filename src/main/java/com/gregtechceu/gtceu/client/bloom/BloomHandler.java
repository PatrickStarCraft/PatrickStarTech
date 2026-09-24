package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.particle.GTParticle;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.client.bloom.BloomRenderer.BLOOM_RENDER_LOCK;

/** Manages persistent bloom effects and submits them through the current level's geometry collector. */
@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@UtilityClass
public class BloomHandler {

    static final Map<RenderType, BloomRenderList> BLOOM_RENDERS = new Object2ObjectOpenHashMap<>();
    static final ArrayList<BloomRenderTicket> SCHEDULED_BLOOM_RENDERS = new ArrayList<>();

    /**
     * Register a bloom callback until its ticket or owner becomes invalid. Its geometry is submitted using the
     * pipeline returned by {@code setup}; passing {@code null} selects the default bloom pipeline.
     */
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup, IBloomEffect render,
                                                        BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity == null");
        return registerBloomRender(setup,
                new IBloomEffect() {
                    @Override
                    public void submitBloomEffect(PoseStack poseStack, SubmitNodeCollector collector,
                                                  RenderType renderType, EffectRenderContext context) {
                        render.submitBloomEffect(poseStack, collector, renderType, context);
                    }

                    @Override
                    public boolean shouldRenderBloomEffect(EffectRenderContext context) {
                        return blockEntity.getLevel() == context.getRenderViewEntity().level() &&
                                render.shouldRenderBloomEffect(context);
                    }
                },
                ticket -> !blockEntity.isRemoved(), blockEntity::getLevel);
    }

    /** Register a bloom callback while a particle remains alive. */
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup, IBloomEffect render,
                                                        GTParticle particle) {
        Objects.requireNonNull(particle, "particle == null");
        return registerBloomRender(setup, render, ticket -> particle.isAlive());
    }

    /** Register a persistent bloom callback with an optional ticket validity predicate. */
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup, IBloomEffect render,
                                                        @Nullable Predicate<BloomRenderTicket> validityChecker) {
        return registerBloomRender(setup, render, validityChecker, null);
    }

    /**
     * Register a persistent bloom callback. The optional world supplier is consulted for unload cleanup; the callback
     * itself receives only the frame context and target submission interfaces.
     */
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup, IBloomEffect render,
                                                        @Nullable Predicate<BloomRenderTicket> validityChecker,
                                                        @Nullable Supplier<@Nullable Level> worldContext) {
        Objects.requireNonNull(render, "render == null");
        if (!BloomShaderManager.isBloomActive()) return BloomRenderTicket.INVALID;

        RenderType renderType = setup == null ? GTRenderTypes.bloom() : setup.renderType();
        BloomRenderTicket ticket = new BloomRenderTicket(renderType, render, validityChecker, worldContext);
        BLOOM_RENDER_LOCK.writeLock().lock();
        try {
            SCHEDULED_BLOOM_RENDERS.add(ticket);
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
        }
        return ticket;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void submitBloomRenders(SubmitCustomGeometryEvent event) {
        if (!BloomShaderManager.isBloomActive()) return;

        BLOOM_RENDER_LOCK.writeLock().lock();
        try {
            initializeScheduledRenders();
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
        }

        if (BLOOM_RENDERS.isEmpty()) return;

        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        Minecraft minecraft = Minecraft.getInstance();
        float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Entity cameraEntity = minecraft.getCameraEntity();
        EffectRenderContext context = EffectRenderContext.getInstance()
                .update(camera, camera.cullFrustum, partialTicks, cameraEntity);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.pos.x(), -camera.pos.y(), -camera.pos.z());

        BLOOM_RENDER_LOCK.readLock().lock();
        try {
            for (BloomRenderList renderList : BLOOM_RENDERS.values()) {
                renderList.submit(poseStack, event.getSubmitNodeCollector(), context);
            }
        } finally {
            BLOOM_RENDER_LOCK.readLock().unlock();
            poseStack.popPose();
        }

        BLOOM_RENDER_LOCK.writeLock().lock();
        try {
            removeInvalidatedRenders();
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
        }
    }

    /** Invalidate tickets associated with a world that is unloading. */
    static void invalidateLevelData(LevelAccessor level) {
        Objects.requireNonNull(level, "level == null");
        BLOOM_RENDER_LOCK.writeLock().lock();
        try {
            for (BloomRenderTicket ticket : SCHEDULED_BLOOM_RENDERS) {
                invalidateForLevel(ticket, level);
            }
            for (BloomRenderList list : BLOOM_RENDERS.values()) {
                for (BloomRenderTicket ticket : list) {
                    invalidateForLevel(ticket, level);
                }
            }
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
        }
    }

    private static void invalidateForLevel(BloomRenderTicket ticket, LevelAccessor level) {
        if (ticket.isValid() && ticket.worldContext != null && ticket.worldContext.get() == level) {
            ticket.invalidate();
        }
    }

    private static void initializeScheduledRenders() {
        for (BloomRenderTicket ticket : SCHEDULED_BLOOM_RENDERS) {
            if (!ticket.isValid()) continue;
            BLOOM_RENDERS.computeIfAbsent(ticket.renderType, BloomRenderList::new).add(ticket);
        }
        SCHEDULED_BLOOM_RENDERS.clear();
    }

    private static void removeInvalidatedRenders() {
        BLOOM_RENDERS.values().removeIf(BloomRenderList::postDraw);
    }

    static final class BloomRenderList extends ArrayList<BloomRenderTicket> {

        private final RenderType renderType;

        BloomRenderList(RenderType renderType) {
            super(2);
            this.renderType = renderType;
        }

        void submit(PoseStack poseStack, SubmitNodeCollector collector, EffectRenderContext context) {
            for (BloomRenderTicket ticket : this) {
                ticket.checkValidity();
                if (!ticket.isValid() || !ticket.render.shouldRenderBloomEffect(context)) continue;

                poseStack.pushPose();
                try {
                    ticket.render.submitBloomEffect(poseStack, collector, this.renderType, context);
                } finally {
                    poseStack.popPose();
                }
            }
        }

        boolean postDraw() {
            this.removeIf(ticket -> {
                ticket.checkValidity();
                return !ticket.isValid();
            });
            return this.isEmpty();
        }
    }
}
