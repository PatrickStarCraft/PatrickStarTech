package com.gregtechceu.gtceu.integration.map.xaeros.worldmap.ore;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.worldgen.ores.GeneratedVeinMetadata;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.map.GroupingMapRenderer;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;
import com.gregtechceu.gtceu.integration.map.xaeros.common.ore.OreVeinElement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.lib.client.graphics.util.ImmediateRenderUtil;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public class OreVeinElementRenderer extends
                                    ElementRenderer<OreVeinElement, OreVeinElementContext, OreVeinElementRenderer> {

    protected OreVeinElementRenderer(OreVeinElementContext context,
                                     ElementRenderProvider<OreVeinElement, OreVeinElementContext> provider,
                                     ElementReader<OreVeinElement, OreVeinElementContext, OreVeinElementRenderer> reader) {
        super(context, provider, reader);
    }

    @Override
    public void preRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers,
                          MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {}

    @Override
    public void postRender(ElementRenderInfo renderInfo, XaeroBufferProvider buffers,
                           MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {}

    @Override
    public void renderElementShadow(OreVeinElement element, boolean hovered, float brightness,
                                   double optionalScale, double screenSizeBasedScale,
                                   ElementRenderInfo renderInfo, MapElementGraphics graphics,
                                   XaeroBufferProvider buffers,
                                   MultiTextureRenderTypeRendererProvider rendererProvider) {}

    @Override
    public boolean renderElement(OreVeinElement element, boolean hovered,
                                double optionalDepth, float optionalScale,
                                double partialX, double partialY, ElementRenderInfo renderInfo,
                                MapElementGraphics graphics, XaeroBufferProvider buffers,
                                MultiTextureRenderTypeRendererProvider rendererProvider) {
        GeneratedVeinMetadata vein = element.getVein();
        int iconSize = ConfigHolder.INSTANCE.compat.minimap.oreIconSize;

        Material material = OreRenderLayer.getMaterial(vein);
        int materialARGB = material.getMaterialARGB();
        float[] colors = RenderUtil.floats(materialARGB);
        ImmediateRenderUtil.setShaderColor(colors[0], colors[1], colors[2], colors[3]);

        Identifier oreTexture = MaterialIconType.rawOre
                .getItemTexturePath(material.getMaterialIconSet(), true);
        if (oreTexture != null) {
            TextureAtlasSprite oreSprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(oreTexture);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.blit(oreSprite, -iconSize / 2, -iconSize / 2, iconSize, iconSize,
                    RenderPipelines.GUI_TEXTURED);
            graphics.pose().popPose();
        }

        oreTexture = MaterialIconType.rawOre.getItemTexturePath(material.getMaterialIconSet(), "secondary", true);
        if (oreTexture != null) {
            int materialSecondaryARGB = material.getMaterialSecondaryARGB();
            colors = RenderUtil.floats(materialSecondaryARGB);
            ImmediateRenderUtil.setShaderColor(colors[0], colors[1], colors[2], colors[3]);
            TextureAtlasSprite oreSprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(oreTexture);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.blit(oreSprite, -iconSize / 2, -iconSize / 2, iconSize, iconSize,
                    RenderPipelines.GUI_TEXTURED);
            graphics.pose().popPose();
        }

        ImmediateRenderUtil.setShaderColor(1, 1, 1, 1);
        int borderColor = ConfigHolder.INSTANCE.compat.minimap.getBorderColor(materialARGB | 0xFF000000);
        if ((borderColor & 0xFF000000) != 0) {
            int thickness = iconSize / 16;
            graphics.fill(-iconSize / 2, -iconSize / 2, iconSize, thickness, borderColor);
            graphics.fill(-iconSize / 2, -iconSize / 2 - thickness, iconSize, thickness, borderColor);
            graphics.fill(-iconSize / 2, -iconSize / 2, thickness, iconSize, borderColor);
            graphics.fill(-iconSize / 2 - thickness, -iconSize / 2, thickness, iconSize, borderColor);
        }
        return true;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean pre) {
        return GroupingMapRenderer.getInstance().doShowLayer("ore_veins");
    }

    public static final class Builder {

        private Builder() {}

        public OreVeinElementRenderer build() {
            return new OreVeinElementRenderer(new OreVeinElementContext(), new OreVeinElementRenderProvider(),
                    new OreVeinElementReader());
        }

        public static OreVeinElementRenderer.Builder begin() {
            return new OreVeinElementRenderer.Builder();
        }
    }
}
