package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.client.bloom.BloomShaderManager;
import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
public final class GTRenderTypes {

    private static final OutputTarget BLOOM_TARGET = new OutputTarget("gtceu_bloom", () ->
            BloomShaderManager.isBloomActive() ? BloomShaderManager.BLOOM_TARGET : null);

    private static final RenderPipeline LIGHT_RING_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(GTCEu.id("pipeline/light_ring"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build();

    private static final RenderPipeline BLOOM_LIGHT_RING_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(GTCEu.id("pipeline/bloom_light_ring"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .build();

    private static final RenderPipeline BLOOM_PIPELINE = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
            .withLocation(GTCEu.id("pipeline/bloom"))
            .withVertexShader(GTCEu.id("core/rendertype_bloom"))
            .withFragmentShader(GTCEu.id("core/rendertype_bloom"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    private static final RenderPipeline ENTITY_BLOOM_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(GTCEu.id("pipeline/entity_bloom"))
            .withVertexShader(GTCEu.id("core/rendertype_entity_bloom"))
            .withFragmentShader(GTCEu.id("core/rendertype_entity_bloom"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    private static final RenderPipeline MONITOR_PIPELINE = colorPipeline("monitor", PrimitiveTopology.QUADS,
            RenderPipelines.MATRICES_FOG_SNIPPET, false, BlendFunction.TRANSLUCENT, Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline ASSEMBLY_LINE_PIPELINE = colorPipeline("assembly_line", PrimitiveTopology.QUADS,
            RenderPipelines.MATRICES_FOG_SNIPPET, true, BlendFunction.TRANSLUCENT, Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline PARTICLE_PIPELINE = colorPipeline("particle", PrimitiveTopology.QUADS,
            RenderPipelines.MATRICES_FOG_SNIPPET, false, BlendFunction.TRANSLUCENT,
            Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline PARTICLE_NO_DEPTH_WRITE_PIPELINE = colorPipeline("particle_no_depth_write",
            PrimitiveTopology.QUADS, RenderPipelines.MATRICES_FOG_SNIPPET, false, BlendFunction.TRANSLUCENT,
            Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)));
    private static final RenderPipeline BLOOM_PARTICLE_PIPELINE = colorPipeline("bloom_particle",
            PrimitiveTopology.QUADS, RenderPipelines.MATRICES_FOG_SNIPPET, false, null,
            Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline HIGHLIGHT_PIPELINE = colorPipeline("block_highlight_quads", PrimitiveTopology.QUADS,
            RenderPipelines.DEBUG_FILLED_SNIPPET, false, null, Optional.empty());
    private static final RenderPipeline GUI_TRIANGLE_STRIP_PIPELINE = guiPipeline("gui_triangle_strip", PrimitiveTopology.TRIANGLE_STRIP,
            RenderPipelines.GUI_SNIPPET, Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline GUI_TRIANGLE_FAN_PIPELINE = guiPipeline("gui_triangle_fan", PrimitiveTopology.TRIANGLE_FAN,
            RenderPipelines.GUI_SNIPPET, Optional.of(DepthStencilState.DEFAULT));
    private static final RenderPipeline GUI_OVERLAY_TRIANGLE_FAN_PIPELINE = guiPipeline("gui_overlay_triangle_fan",
            PrimitiveTopology.TRIANGLE_FAN, RenderPipelines.GUI_SNIPPET, Optional.empty());
    private static final RenderPipeline GUI_TEXTURE_TRIANGLE_STRIP_PIPELINE = guiPipeline("gui_texture_triangle_strip",
            PrimitiveTopology.TRIANGLE_STRIP, RenderPipelines.GUI_TEXTURED_SNIPPET, Optional.of(DepthStencilState.DEFAULT));

    private static final List<RenderPipeline> PIPELINES = List.of(
            LIGHT_RING_PIPELINE, BLOOM_LIGHT_RING_PIPELINE, BLOOM_PIPELINE, ENTITY_BLOOM_PIPELINE, MONITOR_PIPELINE,
            ASSEMBLY_LINE_PIPELINE,
            PARTICLE_PIPELINE, PARTICLE_NO_DEPTH_WRITE_PIPELINE, BLOOM_PARTICLE_PIPELINE, HIGHLIGHT_PIPELINE,
            GUI_TRIANGLE_STRIP_PIPELINE, GUI_TRIANGLE_FAN_PIPELINE, GUI_OVERLAY_TRIANGLE_FAN_PIPELINE,
            GUI_TEXTURE_TRIANGLE_STRIP_PIPELINE);

    private static final RenderType LIGHT_RING = create("light_ring", RenderSetup.builder(LIGHT_RING_PIPELINE).createRenderSetup());
    private static final RenderType BLOOM_LIGHT_RING = create("bloom_light_ring", RenderSetup.builder(BLOOM_LIGHT_RING_PIPELINE)
            .setOutputTarget(BLOOM_TARGET).createRenderSetup());
    private static final RenderType BLOOM = create("bloom", RenderSetup.builder(BLOOM_PIPELINE)
            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
            .useLightmap()
            .setOutputTarget(BLOOM_TARGET)
            .createRenderSetup());
    private static final Function<Identifier, RenderType> ENTITY_BLOOM = Util.memoize(texture -> create("entity_bloom",
            RenderSetup.builder(ENTITY_BLOOM_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .setOutputTarget(BLOOM_TARGET)
                    .createRenderSetup()));
    private static final RenderType MONITOR = create("central_monitor", RenderSetup.builder(MONITOR_PIPELINE).createRenderSetup());
    private static final RenderType GUI_COLOR = create("gui_color", RenderSetup.builder(RenderPipelines.GUI).createRenderSetup());
    private static final RenderType BLOCK_HIGHLIGHT_QUADS = create("gt_block_highlight_quads",
            RenderSetup.builder(HIGHLIGHT_PIPELINE).createRenderSetup());
    private static final RenderType ASSEMBLY_LINE = create("assembly_line", RenderSetup.builder(ASSEMBLY_LINE_PIPELINE).createRenderSetup());
    private static final RenderType PARTICLE = create("particle", RenderSetup.builder(PARTICLE_PIPELINE).createRenderSetup());
    private static final RenderType PARTICLE_NO_DEPTH_WRITE = create("particle_no_depth_write",
            RenderSetup.builder(PARTICLE_NO_DEPTH_WRITE_PIPELINE).createRenderSetup());
    private static final RenderType BLOOM_PARTICLE = create("bloom_particle",
            RenderSetup.builder(BLOOM_PARTICLE_PIPELINE).setOutputTarget(BLOOM_TARGET).createRenderSetup());
    private static final Function<Identifier, RenderType> GUI_TEXTURE = Util.memoize(texture -> create("gui_texture",
            RenderSetup.builder(RenderPipelines.GUI_TEXTURED).withTexture("Sampler0", texture).createRenderSetup()));
    private static final RenderType GUI_TRIANGLE_STRIP = create("gui_triangle_strip",
            RenderSetup.builder(GUI_TRIANGLE_STRIP_PIPELINE).createRenderSetup());
    private static final RenderType GUI_TRIANGLE_FAN = create("gui_triangle_fan",
            RenderSetup.builder(GUI_TRIANGLE_FAN_PIPELINE).createRenderSetup());
    private static final RenderType GUI_OVERLAY_TRIANGLE_FAN = create("gui_overlay_triangle_fan",
            RenderSetup.builder(GUI_OVERLAY_TRIANGLE_FAN_PIPELINE).createRenderSetup());
    private static final Function<Identifier, RenderType> GUI_TEXTURE_TRIANGLE_STRIP = Util.memoize(texture -> create(
            "gui_texture_triangle_strip", RenderSetup.builder(GUI_TEXTURE_TRIANGLE_STRIP_PIPELINE)
                    .withTexture("Sampler0", texture).createRenderSetup()));

    private static final RenderPipeline INWORLD_GUI_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(GTCEu.id("pipeline/inworld_gui"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();
    private static final RenderType INWORLD_GUI = create("inworld_gui", RenderSetup.builder(INWORLD_GUI_PIPELINE).createRenderSetup());

    @SubscribeEvent
    private static void registerPipelines(RegisterRenderPipelinesEvent event) {
        PIPELINES.forEach(event::registerPipeline);
        event.registerPipeline(INWORLD_GUI_PIPELINE);
    }

    private static RenderPipeline colorPipeline(String name, PrimitiveTopology topology, RenderPipeline.Snippet snippet,
                                                boolean cull, BlendFunction blend, Optional<DepthStencilState> depthState) {
        RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
                .withLocation(GTCEu.id("pipeline/" + name))
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(topology)
                .withCull(cull)
                .withDepthStencilState(depthState);
        if (blend != null) {
            builder.withColorTargetState(new ColorTargetState(blend));
        }
        return builder.build();
    }

    private static RenderPipeline guiPipeline(String name, PrimitiveTopology topology, RenderPipeline.Snippet snippet,
                                              Optional<DepthStencilState> depthState) {
        return RenderPipeline.builder(snippet)
                .withLocation(GTCEu.id("pipeline/" + name))
                .withPrimitiveTopology(topology)
                .withDepthStencilState(depthState)
                .build();
    }

    private static RenderType create(String name, RenderSetup setup) {
        return RenderType.create("gtceu:" + name, setup);
    }

    public static RenderType lightRing() {
        return LIGHT_RING;
    }

    public static RenderType bloomLightRing() {
        return BLOOM_LIGHT_RING;
    }


    public static RenderType bloom() {
        return BLOOM;
    }

    public static RenderType entityBloom(Identifier location) {
        return ENTITY_BLOOM.apply(location);
    }

    @SuppressWarnings("deprecation")
    public static RenderType entityBloomBlockSheet() {
        return entityBloom(TextureAtlas.LOCATION_BLOCKS);
    }

    public static RenderType blockHighlightQuads() {
        return BLOCK_HIGHLIGHT_QUADS;
    }

    public static RenderType assemblyLine() {
        return ASSEMBLY_LINE;
    }

    public static RenderType bloomParticle() {
        return BLOOM_PARTICLE;
    }

    public static RenderType particle(boolean writeDepth) {
        return writeDepth ? PARTICLE : PARTICLE_NO_DEPTH_WRITE;
    }

    public static RenderType getMonitor() {
        return MONITOR;
    }

    public static RenderType guiColor() {
        return GUI_COLOR;
    }

    public static RenderType guiTexture(Identifier texture) {
        return GUI_TEXTURE.apply(texture);
    }

    public static RenderType inWorldGui() {
        return INWORLD_GUI;
    }

    public static RenderType guiTriangleStrip() {
        return GUI_TRIANGLE_STRIP;
    }

    public static RenderType guiTriangleStrip(Identifier texture) {
        return GUI_TEXTURE_TRIANGLE_STRIP.apply(texture);
    }

    public static RenderType guiTriangleFan() {
        return GUI_TRIANGLE_FAN;
    }

    public static RenderType guiOverlayTriangleFan() {
        return GUI_OVERLAY_TRIANGLE_FAN;
    }

    private GTRenderTypes() {}
}
