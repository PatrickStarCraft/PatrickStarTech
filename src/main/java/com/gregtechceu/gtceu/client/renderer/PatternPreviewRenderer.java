package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.mui.schema.MutableSchema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.model.data.ModelData;

import brachy.modularui.drawable.schema.*;
import brachy.modularui.integration.embeddium.SodiumCompat;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public class PatternPreviewRenderer {

    public static final PatternPreviewRenderer INSTANCE = new PatternPreviewRenderer();

    private static final ContextKey<PatternPreviewRenderData> RENDER_DATA =
            new ContextKey<>(GTCEu.id("pattern_preview"));
    private static final PatternPreviewRenderData EMPTY_RENDER_DATA =
            new PatternPreviewRenderData(BlockPos.ZERO, List.of(), Map.of(), List.of(), Set.of());

    private @Nullable MutableSchema schema;
    private @Nullable RenderLevel renderLevel;
    private @Nullable BlockPos controllerPos;
    private final AtomicInteger timeout = new AtomicInteger(-1);
    private @Nullable PreviewGeometry previewGeometry;
    private volatile boolean dirty = true;

    public void showPreview(BlockPos controllerPos, MutableSchema schema, RenderFilter renderFilter, int duration) {
        this.controllerPos = controllerPos;
        this.schema = schema;
        this.renderLevel = new RenderLevel(schema, renderFilter);
        timeout.set(duration);

        notifyRecompile();
    }

    // Layer to show in the render preview, MAX_INT is show all
    private int layers = Integer.MAX_VALUE;

    public void showPreviewCycleLevel(BlockPos controllerPos, MutableSchema schema, int duration) {
        if (controllerPos.equals(this.controllerPos)) {
            var bounds = schema.getBounds();
            int min = bounds.getFirst().getY();
            int max = bounds.getSecond().getY();
            if (layers == Integer.MAX_VALUE) {
                layers = min;
            } else {
                layers += 1;
                if (layers > max) {
                    layers = Integer.MAX_VALUE;
                }
            }
        } else {
            layers = Integer.MAX_VALUE;
        }
        RenderFilter renderFilter;
        if (layers == Integer.MAX_VALUE) {
            renderFilter = RenderFilter.ALL;
        } else {
            renderFilter = (pos, state) -> pos.getY() == layers;
        }
        showPreview(controllerPos, schema, renderFilter, duration);
    }

    public void clientTick() {
        if (timeout.get() > 0 && timeout.decrementAndGet() <= 0) {
            dispose();
        }
    }

    public void notifyRecompile() {
        this.dirty = true;
    }

    @SubscribeEvent
    public static void registerReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(GTCEu.id("pattern_preview"),
                (ResourceManagerReloadListener) resourceManager -> INSTANCE.notifyRecompile());
    }

    public void dispose() {
        this.schema = null;
        this.renderLevel = null;
        this.controllerPos = null;
        this.previewGeometry = null;
        this.timeout.set(-1);

        this.dirty = false;
    }

    @SubscribeEvent
    public static void extractPreview(ExtractLevelRenderStateEvent event) {
        PatternPreviewRenderer renderer = INSTANCE;
        MutableSchema schema = renderer.schema;
        RenderLevel renderLevel = renderer.renderLevel;
        BlockPos controllerPos = renderer.controllerPos;
        if (renderer.timeout.get() <= 0 || schema == null || renderLevel == null || controllerPos == null) {
            event.getRenderState().setRenderData(RENDER_DATA, EMPTY_RENDER_DATA);
            return;
        }

        PreviewBlockAndTintGetter previewLevel = new PreviewBlockAndTintGetter(
                renderLevel, event.getLevel(), controllerPos);
        if (renderer.dirty || renderer.previewGeometry == null) {
            renderer.previewGeometry = renderer.buildPreviewGeometry(schema, renderLevel, previewLevel);
            renderer.dirty = false;
        }

        Vec3 cameraPos = event.getRenderState().cameraRenderState.pos;
        Vec3 previewCameraPos = new Vec3(cameraPos.x - controllerPos.getX(),
                cameraPos.y - controllerPos.getY(), cameraPos.z - controllerPos.getZ());
        float partialTicks = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<PreviewBlockEntity> blockEntities = renderer.extractBlockEntities(schema, renderLevel,
                previewCameraPos, partialTicks);
        PreviewGeometry geometry = renderer.previewGeometry;
        event.getRenderState().setRenderData(RENDER_DATA, new PatternPreviewRenderData(controllerPos,
                geometry.blocks(), geometry.fluids(), blockEntities, geometry.activeFluidSprites()));
    }

    @SubscribeEvent
    public static void submitPreview(SubmitCustomGeometryEvent event) {
        PatternPreviewRenderData renderData = event.getLevelRenderState().getRenderData(RENDER_DATA);
        if (renderData == null || renderData.isEmpty()) return;

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        BlockPos controllerPos = renderData.controllerPos();
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        if (!renderData.activeFluidSprites().isEmpty()) {
            SodiumCompat.markSpritesAsActive(renderData.activeFluidSprites());
        }

        for (PreviewBlock block : renderData.blocks()) {
            BlockPos pos = controllerPos.offset(block.pos());
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            block.modelState().submitMultiLayer(poseStack, collector, block.lightCoords(),
                    OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        for (Map.Entry<ChunkSectionLayer, List<PreviewFluidVertex>> fluidLayer : renderData.fluids().entrySet()) {
            if (fluidLayer.getValue().isEmpty()) continue;
            poseStack.pushPose();
            poseStack.translate(controllerPos.getX() - cameraPos.x,
                    controllerPos.getY() - cameraPos.y, controllerPos.getZ() - cameraPos.z);
            RenderType renderType = renderTypeFor(fluidLayer.getKey());
            List<PreviewFluidVertex> vertices = fluidLayer.getValue();
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (PreviewFluidVertex vertex : vertices) {
                    buffer.addVertex(pose.pose(), vertex.x(), vertex.y(), vertex.z())
                            .setColor(vertex.color())
                            .setUv(vertex.u(), vertex.v())
                            .setUv1(vertex.overlayU(), vertex.overlayV())
                            .setUv2(vertex.lightU(), vertex.lightV())
                            .setNormal(pose, vertex.normalX(), vertex.normalY(), vertex.normalZ());
                }
            });
            poseStack.popPose();
        }

        BlockEntityRenderDispatcher blockEntityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (PreviewBlockEntity blockEntity : renderData.blockEntities()) {
            BlockPos pos = controllerPos.offset(blockEntity.pos());
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            blockEntityDispatcher.submit(blockEntity.state(), poseStack, collector,
                    event.getLevelRenderState().cameraRenderState);
            poseStack.popPose();
        }
    }

    private PreviewGeometry buildPreviewGeometry(MutableSchema schema, RenderLevel renderLevel,
                                                BlockAndTintGetter previewLevel) {
        List<PreviewBlock> blocks = new ArrayList<>();
        Map<ChunkSectionLayer, List<PreviewFluidVertex>> fluids = new EnumMap<>(ChunkSectionLayer.class);
        Set<TextureAtlasSprite> activeFluidSprites = new HashSet<>();

        Minecraft minecraft = Minecraft.getInstance();
        BlockColors blockColors = minecraft.getBlockColors();
        var modelSet = minecraft.getModelManager().getBlockStateModelSet();
        var fluidModels = minecraft.getModelManager().getFluidStateModelSet();
        FluidRenderer fluidRenderer = new FluidRenderer(fluidModels);
        for (var blockEntry : schema) {
            BlockPos pos = blockEntry.getKey();
            BlockState blockState = renderLevel.getBlockState(pos);
            if (blockState.isAir()) continue;

            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                FluidModel fluidModel = fluidModels.get(fluidState);
                activeFluidSprites.add(fluidModel.stillMaterial().sprite());
                activeFluidSprites.add(fluidModel.flowingMaterial().sprite());

                int baseX = (pos.getX() >> 4) << 4;
                int baseY = (pos.getY() >> 4) << 4;
                int baseZ = (pos.getZ() >> 4) << 4;
                Map<ChunkSectionLayer, PreviewFluidVertexConsumer> consumers = new EnumMap<>(ChunkSectionLayer.class);
                fluidRenderer.tesselate(previewLevel, pos,
                        layer -> consumers.computeIfAbsent(layer, key -> new PreviewFluidVertexConsumer(
                                fluids.computeIfAbsent(key, ignored -> new ArrayList<>()), baseX, baseY, baseZ)),
                        blockState, fluidState);
                consumers.values().forEach(PreviewFluidVertexConsumer::finish);
            }

            if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                BlockStateModel model = modelSet.get(blockState);
                RandomSource random = RandomSource.create(blockState.getSeed(pos));
                Vec3 offset = blockState.getOffset(pos);
                Matrix4f transform = new Matrix4f()
                        .translate(0.5F, 0.5F, 0.5F)
                        .scale(0.8F, 0.8F, 0.8F)
                        .translate(-0.5F, -0.5F, -0.5F)
                        .translate((float) offset.x, (float) offset.y, (float) offset.z);
                BlockModelRenderState modelState = new BlockModelRenderState();
                List<BlockStateModelPart> parts = modelState.setupModel(transform,
                        model.hasMaterialFlag(previewLevel, pos, blockState, BakedQuad.FLAG_TRANSLUCENT));
                model.collectParts(previewLevel, pos, blockState, random, parts);

                List<BlockTintSource> tintSources = blockColors.getTintSources(blockState);
                if (tintSources.isEmpty()) {
                    IClientBlockExtensions.of(blockState).collectDynamicTintValues(blockState, previewLevel,
                            pos, modelState.tintLayers());
                } else {
                    for (BlockTintSource tintSource : tintSources) {
                        modelState.tintLayers().add(tintSource.colorInWorld(blockState, previewLevel, pos));
                    }
                }

                modelState.blockLightCoords = blockState.emissiveRendering()
                        ? 15728880
                        : LightCoordsUtil.pack(blockState.getLightEmission(previewLevel, pos), 0);
                int lightCoords = LightCoordsUtil.max(LightCoordsUtil.getLightCoords(previewLevel, pos),
                        modelState.blockLightCoords);
                blocks.add(new PreviewBlock(pos, modelState, lightCoords));
            }
        }

        Map<ChunkSectionLayer, List<PreviewFluidVertex>> frozenFluids = new EnumMap<>(ChunkSectionLayer.class);
        fluids.forEach((layer, vertices) -> frozenFluids.put(layer, List.copyOf(vertices)));
        return new PreviewGeometry(List.copyOf(blocks), Map.copyOf(frozenFluids), Set.copyOf(activeFluidSprites));
    }

    private List<PreviewBlockEntity> extractBlockEntities(MutableSchema schema, RenderLevel renderLevel,
                                                         Vec3 cameraPos, float partialTicks) {
        List<PreviewBlockEntity> renderStates = new ArrayList<>();
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (var blockEntry : schema) {
            BlockPos pos = blockEntry.getKey();
            if (renderLevel.getBlockState(pos).isAir()) continue;
            BlockEntity blockEntity = renderLevel.getBlockEntity(pos);
            if (blockEntity == null) continue;
            PreviewBlockEntity renderState = extractBlockEntity(dispatcher, blockEntity, cameraPos, partialTicks);
            if (renderState != null) renderStates.add(renderState);
        }
        return List.copyOf(renderStates);
    }

    private static <T extends BlockEntity, S extends BlockEntityRenderState> PreviewBlockEntity extractBlockEntity(
            BlockEntityRenderDispatcher dispatcher, T blockEntity, Vec3 cameraPos, float partialTicks) {
        BlockEntityRenderer<T, S> renderer = dispatcher.getRenderer(blockEntity);
        if (renderer == null) return null;
        S renderState = renderer.createRenderState();
        renderer.extractRenderState(blockEntity, renderState, partialTicks, cameraPos, null);
        return new PreviewBlockEntity(blockEntity.getBlockPos(), renderState);
    }

    private static RenderType renderTypeFor(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PatternPreviewRenderer that)) return false;

        return Objects.equals(this.schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.schema);
    }

    private record PreviewGeometry(List<PreviewBlock> blocks,
                                   Map<ChunkSectionLayer, List<PreviewFluidVertex>> fluids,
                                   Set<TextureAtlasSprite> activeFluidSprites) {}

    private record PreviewBlock(BlockPos pos, BlockModelRenderState modelState, int lightCoords) {}

    private record PreviewBlockEntity(BlockPos pos, BlockEntityRenderState state) {}

    private record PatternPreviewRenderData(BlockPos controllerPos, List<PreviewBlock> blocks,
                                            Map<ChunkSectionLayer, List<PreviewFluidVertex>> fluids,
                                            List<PreviewBlockEntity> blockEntities,
                                            Set<TextureAtlasSprite> activeFluidSprites) {
        private boolean isEmpty() {
            return this.blocks.isEmpty() && this.fluids.isEmpty() && this.blockEntities.isEmpty();
        }
    }

    private record PreviewFluidVertex(float x, float y, float z, int color, float u, float v,
                                      int overlayU, int overlayV, int lightU, int lightV,
                                      float normalX, float normalY, float normalZ) {}

    private static final class PreviewBlockAndTintGetter implements BlockAndTintGetter {
        private final RenderLevel previewLevel;
        private final net.minecraft.client.multiplayer.ClientLevel clientLevel;
        private final BlockPos controllerPos;

        private PreviewBlockAndTintGetter(RenderLevel previewLevel,
                                          net.minecraft.client.multiplayer.ClientLevel clientLevel,
                                          BlockPos controllerPos) {
            this.previewLevel = previewLevel;
            this.clientLevel = clientLevel;
            this.controllerPos = controllerPos;
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return this.clientLevel.cardinalLighting();
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.clientLevel.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return this.clientLevel.getBlockTint(this.controllerPos.offset(pos), colorResolver);
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return this.clientLevel.getBrightness(layer, this.controllerPos.offset(pos));
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return this.clientLevel.getRawBrightness(this.controllerPos.offset(pos), darkening);
        }

        @Override
        public boolean canSeeSky(BlockPos pos) {
            return this.clientLevel.canSeeSky(this.controllerPos.offset(pos));
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return this.previewLevel.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.previewLevel.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.previewLevel.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return this.previewLevel.getHeight();
        }

        @Override
        public int getMinY() {
            return this.previewLevel.getMinY();
        }

        @Override
        public int getMaxY() {
            return this.previewLevel.getMaxY();
        }

        @Override
        public ModelData getModelData(BlockPos pos) {
            return this.previewLevel.getModelData(pos);
        }
    }

    private static final class PreviewFluidVertexConsumer implements VertexConsumer {
        private final List<PreviewFluidVertex> output;
        private final int baseX;
        private final int baseY;
        private final int baseZ;
        private boolean hasVertex;
        private float x;
        private float y;
        private float z;
        private int color = -1;
        private float u;
        private float v;
        private int overlayU;
        private int overlayV;
        private int lightU;
        private int lightV;
        private float normalX;
        private float normalY = 1.0F;
        private float normalZ;

        private PreviewFluidVertexConsumer(List<PreviewFluidVertex> output, int baseX, int baseY, int baseZ) {
            this.output = output;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.finishCurrent();
            this.x = x + this.baseX;
            this.y = y + this.baseY;
            this.z = z + this.baseZ;
            this.color = -1;
            this.u = this.v = 0.0F;
            this.overlayU = this.overlayV = this.lightU = this.lightV = 0;
            this.normalX = this.normalZ = 0.0F;
            this.normalY = 1.0F;
            this.hasVertex = true;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.color = ARGB.color(alpha, red, green, blue);
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
            this.overlayU = u;
            this.overlayV = v;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.lightU = u;
            this.lightV = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.normalX = x;
            this.normalY = y;
            this.normalZ = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        private void finish() {
            this.finishCurrent();
        }

        private void finishCurrent() {
            if (!this.hasVertex) return;
            this.output.add(new PreviewFluidVertex(this.x, this.y, this.z, this.color, this.u, this.v,
                    this.overlayU, this.overlayV, this.lightU, this.lightV,
                    this.normalX, this.normalY, this.normalZ));
            this.hasVertex = false;
        }
    }
}
