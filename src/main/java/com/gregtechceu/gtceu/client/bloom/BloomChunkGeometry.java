package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.util.TextureMetadataHelper;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable, reload-scoped bloom geometry captured from the same model parts and lighting as chunk compilation. */
final class BloomChunkGeometry {

    private static final Object LIFECYCLE_LOCK = new Object();
    private static final Map<SectionPos, Snapshot> SECTIONS = new ConcurrentHashMap<>();
    private static final Map<SectionPos, Long> REVISIONS = new ConcurrentHashMap<>();
    private static long worldGeneration;

    private BloomChunkGeometry() {}

    static BuildToken beginRebuild(SectionPos section) {
        synchronized (LIFECYCLE_LOCK) {
            long revision = REVISIONS.merge(section, 1L, Long::sum);
            return new BuildToken(worldGeneration, revision);
        }
    }

    static void publish(SectionPos section, BuildToken token, Snapshot snapshot) {
        synchronized (LIFECYCLE_LOCK) {
            if (token.worldGeneration != worldGeneration ||
                    REVISIONS.getOrDefault(section, 0L) != token.revision) {
                return;
            }
            if (snapshot.isEmpty()) {
                SECTIONS.remove(section);
            } else {
                SECTIONS.put(section, snapshot);
            }
        }
    }

    static void publishCompleted(SectionPos section, Snapshot snapshot) {
        synchronized (LIFECYCLE_LOCK) {
            long revision = REVISIONS.merge(section, 1L, Long::sum);
            publish(section, new BuildToken(worldGeneration, revision), snapshot);
        }
    }

    static void invalidateSection(SectionPos section) {
        synchronized (LIFECYCLE_LOCK) {
            REVISIONS.merge(section, 1L, Long::sum);
            SECTIONS.remove(section);
        }
    }

    static void invalidateAll() {
        synchronized (LIFECYCLE_LOCK) {
            worldGeneration++;
            REVISIONS.clear();
            SECTIONS.clear();
        }
    }

    static void addSectionRenderer(AddSectionGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getLevel() != minecraft.level) return;
        SectionPos section = SectionPos.of(event.getSectionOrigin());
        BuildToken token = beginRebuild(section);
        if (!BloomShaderManager.isBloomActive()) {
            publish(section, token, Snapshot.EMPTY);
            return;
        }

        var modelSet = minecraft.getModelManager().getBlockStateModelSet();
        event.addRenderer(context -> {
            Builder builder = new Builder();
            BlockAndTintGetter region = context.getRegion();
            ModelBlockRenderer renderer = context.getBlockRenderer();
            BlockPos min = section.origin();
            BlockPos max = min.offset(15, 15, 15);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            BlockQuadOutput output = builder::putBlockQuad;

            for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
                pos.set(blockPos);
                BlockState state = region.getBlockState(pos);
                if (state.getRenderShape() != RenderShape.MODEL) continue;

                renderer.tesselateBlock(
                        output,
                        SectionPos.sectionRelative(pos.getX()),
                        SectionPos.sectionRelative(pos.getY()),
                        SectionPos.sectionRelative(pos.getZ()),
                        region,
                        pos,
                        state,
                        modelSet.get(state),
                        state.getSeed(pos));
            }

            publish(section, token, builder.build());
        });
    }

    static void submitVisible(SubmitCustomGeometryEvent event) {
        if (!BloomShaderManager.isBloomActive()) return;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        for (var renderable : event.getRenderableSections()) {
            BlockPos origin = renderable.getRenderOrigin();
            SectionPos section = SectionPos.of(origin);
            Snapshot snapshot = SECTIONS.get(section);
            if (snapshot == null) continue;

            poseStack.pushPose();
            try {
                poseStack.translate(origin.getX() - camera.x(), origin.getY() - camera.y(), origin.getZ() - camera.z());
                collector.submitCustomGeometry(poseStack, GTRenderTypes.bloom(), snapshot::submit);
            } finally {
                poseStack.popPose();
            }
        }
    }

    record BuildToken(long worldGeneration, long revision) {}

    static final class Builder {
        private final FloatArrayList positions = new FloatArrayList();
        private final IntArrayList colors = new IntArrayList();
        private final LongArrayList uvs = new LongArrayList();
        private final IntArrayList lights = new IntArrayList();
        private final IntArrayList overlays = new IntArrayList();
        private final FloatArrayList normals = new FloatArrayList();
        private final int[] ambientLights = new int[4];

        void putBlockQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
            for (int vertex = 0; vertex < 4; vertex++) {
                ambientLights[vertex] = instance.getLightCoords(vertex);
            }
            if (!TextureMetadataHelper.hasBloom(quad, ambientLights)) return;

            Vector3fc normal = quad.direction().getUnitVec3f();
            int emission = quad.materialInfo().lightEmission();
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = quad.position(vertex);
                addVertex(position.x() + x, position.y() + y, position.z() + z,
                        net.minecraft.util.ARGB.multiply(instance.getColor(vertex), quad.bakedColors().color(vertex)),
                        quad.packedUV(vertex), instance.overlayCoords(),
                        instance.getLightCoordsWithEmission(vertex, emission), normal.x(), normal.y(), normal.z());
            }
        }

        void addVertex(float x, float y, float z, int color, long packedUv, int overlay, int light,
                       float normalX, float normalY, float normalZ) {
            this.positions.add(x);
            this.positions.add(y);
            this.positions.add(z);
            this.colors.add(color);
            this.uvs.add(packedUv);
            this.lights.add(light);
            this.overlays.add(overlay);
            this.normals.add(normalX);
            this.normals.add(normalY);
            this.normals.add(normalZ);
        }

        Snapshot build() {
            return this.overlays.isEmpty()
                    ? Snapshot.EMPTY
                    : new Snapshot(this.positions.toFloatArray(), this.colors.toIntArray(), this.uvs.toLongArray(),
                            this.lights.toIntArray(), this.overlays.toIntArray(), this.normals.toFloatArray());
        }
    }

    static final class VertexCollector implements VertexConsumer {
        private final Builder builder = new Builder();
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;
        private float normalX;
        private float normalY;
        private float normalZ;
        private int color = -1;
        private int overlay;
        private int light;
        private boolean hasVertex;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.hasVertex = true;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.color = net.minecraft.util.ARGB.color(alpha, red, green, blue);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.overlay = u & 65535 | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.light = u & 65535 | v << 16;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.normalX = x;
                this.normalY = y;
                this.normalZ = z;
                if (this.hasVertex) {
                    this.builder.addVertex(this.x, this.y, this.z, this.color, UVPair.pack(this.u, this.v), this.overlay,
                            this.light, this.normalX, this.normalY, this.normalZ);
                this.hasVertex = false;
            }
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        Snapshot finish() {
            return this.builder.build();
        }
    }

    static record Snapshot(float[] positions, int[] colors, long[] uvs, int[] lights, int[] overlays,
                           float[] normals) {
        private static final Snapshot EMPTY = new Snapshot(new float[0], new int[0], new long[0], new int[0],
                new int[0], new float[0]);

        boolean isEmpty() {
            return this.overlays.length == 0;
        }

        void submit(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int index = 0; index < this.colors.length; index++) {
                int positionIndex = index * 3;
                long packedUv = this.uvs[index];
                buffer.addVertex(pose, this.positions[positionIndex], this.positions[positionIndex + 1],
                                this.positions[positionIndex + 2])
                        .setColor(this.colors[index])
                        .setUv(UVPair.unpackU(packedUv), UVPair.unpackV(packedUv))
                        .setOverlay(this.overlays[index])
                        .setLight(this.lights[index])
                        .setNormal(pose, this.normals[positionIndex], this.normals[positionIndex + 1],
                                this.normals[positionIndex + 2]);
            }
        }
    }
}
