package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
@NoArgsConstructor
public class AABBHighlightRenderer {

    public static final AABBHighlightRenderer INSTANCE = new AABBHighlightRenderer();

    private static final ContextKey<List<AABBHighlight>> RENDER_DATA =
            new ContextKey<>(GTCEu.id("aabb_highlights"));

    private final List<AABBHighlight> highlights = new ObjectArrayList<>();

    @SubscribeEvent
    public static void extractHighlights(ExtractLevelRenderStateEvent event) {
        long time = System.currentTimeMillis();
        List<AABBHighlight> active = new ArrayList<>();
        synchronized (INSTANCE.highlights) {
            Iterator<AABBHighlight> iterator = INSTANCE.highlights.iterator();
            while (iterator.hasNext()) {
                AABBHighlight highlight = iterator.next();
                long elapsed = time - highlight.startMillis();
                if (elapsed >= highlight.durationMillis()) {
                    iterator.remove();
                    continue;
                }
                if (elapsed < 0) continue;
                if (highlight.phaseMillis() > 0 && elapsed / highlight.phaseMillis() % 2 == 1) continue;
                active.add(highlight);
            }
        }
        event.getRenderState().setRenderData(RENDER_DATA, List.copyOf(active));
    }

    @SubscribeEvent
    public static void submitHighlights(SubmitCustomGeometryEvent event) {
        List<AABBHighlight> active = event.getLevelRenderState().getRenderData(RENDER_DATA);
        if (active == null || active.isEmpty()) return;

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (AABBHighlight highlight : active) {
            AABB relativeBounds = highlight.aabb().move(-camera.x(), -camera.y(), -camera.z());
            collector.submitCustomGeometry(poseStack, GTRenderTypes.blockHighlightQuads(), (pose, buffer) ->
                    RenderBufferHelper.renderAABBOutline(buffer, pose, relativeBounds,
                            highlight.thickness(), highlight.colorARGB()));
        }
    }

    public void addHighlight(AABBHighlight highlight) {
        synchronized (highlights) {
            highlights.add(highlight);
        }
    }

    public record AABBHighlight(AABB aabb, int colorARGB, long startMillis, long durationMillis, long phaseMillis,
                                double thickness) {

        public void remove() {
            synchronized (AABBHighlightRenderer.INSTANCE.highlights) {
                AABBHighlightRenderer.INSTANCE.highlights.remove(this);
            }
        }
    }

    public static AABBHighlightBuilder builder() {
        return new AABBHighlightBuilder();
    }

    @Accessors(chain = true, fluent = true)
    public static class AABBHighlightBuilder {

        @Setter
        private AABB aabb = null;
        @Setter
        private int colorARGB = 0xFFFFFFFF;
        @Setter
        private long startMillis = -1;
        @Setter
        private long durationMillis = 10000;
        @Setter
        private long phaseMillis = 300;
        @Setter
        private double thickness = 0.01D;

        @Tolerate
        public AABBHighlightBuilder aabb(BlockPos pos) {
            this.aabb = new AABB(pos);
            return this;
        }

        public AABBHighlightBuilder colorARGB(int alpha, int red, int green, int blue) {
            this.colorARGB = ARGB.color(alpha, red, green, blue);
            return this;
        }

        public AABBHighlight build() {
            if (aabb == null) {
                throw new IllegalArgumentException("AABB can't be null in AABBHighlightBuilder");
            }
            if (startMillis == -1) {
                startMillis = System.currentTimeMillis();
            }
            return new AABBHighlight(aabb, colorARGB, startMillis, durationMillis, phaseMillis, thickness);
        }
    }
}
