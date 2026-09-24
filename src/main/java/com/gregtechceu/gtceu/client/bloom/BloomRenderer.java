package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.core.config.GTEarlyConfig;
import com.gregtechceu.gtceu.core.mixins.GTMixinPlugin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.SectionPos;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.gregtechceu.gtceu.client.bloom.BloomShaderManager.BLOOM_TARGET;

/** Frame-graph composition for bloom output and the current section-geometry compatibility surface. */
@UtilityClass
public class BloomRenderer {

    static final ReadWriteLock BLOOM_RENDER_LOCK = new ReentrantReadWriteLock();

    static void renderBloom(float partialTicks, net.minecraft.util.profiling.ProfilerFiller profiler) {
        if (!BloomShaderManager.isBloomActive()) return;
        BloomShaderManager.refreshPostChainSettings();
        if (!BloomShaderManager.isBloomActive()) return;

        profiler.push("gtceu:bloom");
        try {
            processPostEffect(partialTicks, profiler);
        } finally {
            profiler.pop();
        }
    }

    private static void processPostEffect(float partialTicks,
                                          net.minecraft.util.profiling.ProfilerFiller profiler) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        RenderTarget bloomSource = BloomShaderManager.BLOOM_TARGET;
        RenderTarget bloomOutput = BloomShaderManager.BLOOM_OUTPUT_TARGET;

        profiler.push("processPostEffect");
        try {
            FrameGraphBuilder frame = new FrameGraphBuilder();
            Map<net.minecraft.resources.Identifier, ResourceHandle<RenderTarget>> targets = new HashMap<>();
            targets.put(PostChain.MAIN_TARGET_ID, frame.importExternal("minecraft:main", mainTarget));
            var sourceId = GTCEu.id("bloom_source");
            var outputId = GTCEu.id("bloom_output");
            targets.put(sourceId, frame.importExternal("gtceu:bloom_source", bloomSource));
            targets.put(outputId, frame.importExternal("gtceu:bloom_output", bloomOutput));

            PostChain.TargetBundle targetBundle = new PostChain.TargetBundle() {
                @Override
                public void replace(net.minecraft.resources.Identifier id, ResourceHandle<RenderTarget> handle) {
                    if (!targets.containsKey(id)) throw new IllegalArgumentException("Unknown bloom target " + id);
                    targets.put(id, handle);
                }

                @Override
                public @Nullable ResourceHandle<RenderTarget> get(net.minecraft.resources.Identifier id) {
                    return targets.get(id);
                }
            };

            BloomShaderManager.BLOOM_CHAIN.addToFrame(frame, mainTarget.width, mainTarget.height, targetBundle);
            frame.execute(((com.gregtechceu.gtceu.core.mixins.client.bloom.GameRendererAccessor)
                    minecraft.gameRenderer).getResourcePool());
            bloomOutput.blitAndBlendToTexture(mainTarget.getColorTextureView(), mainTarget.getDepthTextureView());
        } finally {
            profiler.pop();
        }
    }

    /** Compatibility entry points consumed by the optional Embeddium integration. */
    @UtilityClass
    public static class SafeMode {

        public static final Map<SectionPos, VertexConsumer> BLOOM_BUFFER_BUILDERS = new ConcurrentHashMap<>();

        public static boolean enabled() {
            return GTMixinPlugin.isOptionEnabled(GTEarlyConfig.SAFE_MODE);
        }

        public static VertexConsumer getOrStartBloomBuffer(SectionPos sectionPos) {
            return BLOOM_BUFFER_BUILDERS.computeIfAbsent(sectionPos, ignored -> new BloomChunkGeometry.VertexCollector());
        }

        public static void bakeBloomChunkBuffers(SectionPos sectionPos, float camX, float camY, float camZ) {
            VertexConsumer builder = BLOOM_BUFFER_BUILDERS.remove(sectionPos);
            if (builder instanceof BloomChunkGeometry.VertexCollector collector) {
                BloomChunkGeometry.publishCompleted(sectionPos, collector.finish());
            }
        }

        static void invalidateLevelData() {
            BLOOM_BUFFER_BUILDERS.clear();
            BloomChunkGeometry.invalidateAll();
        }

        public static void invalidateSectionData(SectionPos sectionPos) {
            BLOOM_BUFFER_BUILDERS.remove(sectionPos);
            BloomChunkGeometry.invalidateSection(sectionPos);
        }
    }
}
