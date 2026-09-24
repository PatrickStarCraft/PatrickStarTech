package com.gregtechceu.gtceu.integration.map.xaeros.minimap.ore;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.worldgen.ores.GeneratedVeinMetadata;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.map.GroupingMapRenderer;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;
import com.gregtechceu.gtceu.integration.map.xaeros.common.ore.OreVeinElement;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.lib.client.graphics.util.ImmediateRenderUtil;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

public class OreVeinElementRenderer extends MinimapElementRenderer<OreVeinElement, OreVeinElementContext> {

    private OreVeinElementRenderer(OreVeinElementReader elementReader,
                                   OreVeinElementRenderProvider provider,
                                   OreVeinElementContext context) {
        super(elementReader, provider, context);
    }

    @Override
    public void preRender(MinimapElementRenderInfo renderInfo, XaeroBufferProvider renderTypeBuffers,
                          MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers) {}

    @Override
    public boolean renderElement(OreVeinElement element,
                                 boolean highlit,
                                 boolean outOfBounds,
                                 double optionalDepth, float optionalScale, double partialX, double partialY,
                                 MinimapElementRenderInfo renderInfo,
                                 MinimapElementGraphics graphics, XaeroBufferProvider renderTypeBuffers) {
        GeneratedVeinMetadata vein = element.getVein();
        int iconSize = ConfigHolder.INSTANCE.compat.minimap.oreIconSize;

        Material material = OreRenderLayer.getMaterial(vein);
        int materialARGB = material.getMaterialARGB();
        float[] colors = RenderUtil.floats(materialARGB);
        ImmediateRenderUtil.setShaderColor(colors[0], colors[1], colors[2], colors[3]);

        Identifier oreTexture = MaterialIconType.rawOre
                .getItemTexturePath(material.getMaterialIconSet(), true);
        if (oreTexture != null) {
            var oreSprite = Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(oreTexture);
            graphics.blit(oreSprite, -iconSize / 2, -iconSize / 2, iconSize, iconSize,
                    RenderPipelines.GUI_TEXTURED);
        }
        // FIXME drawing the 2nd layer makes xaero's minimap transparent. so we won't. for now.
        // oreTexture = MaterialIconType.rawOre.getItemTexturePath(firstMaterial.getMaterialIconSet(), "secondary",
        // true);
        // if (oreTexture != null) {
        // int materialSecondaryARGB = firstMaterial.getMaterialSecondaryARGB();
        // colors = DrawUtil.floats(materialSecondaryARGB);
        // var oreSprite = Minecraft.getInstance()
        // .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        // .apply(oreTexture);
        // graphics.blit(-iconSize / 2, -iconSize / 2, 0, iconSize, iconSize,
        // oreSprite, colors[0], colors[1], colors[2], 1);
        // }

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
    public void postRender(MinimapElementRenderInfo renderInfo, XaeroBufferProvider renderTypeBuffers,
                           MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers) {}

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return GroupingMapRenderer.getInstance().doShowLayer("ore_veins") &&
                location == MinimapElementRenderLocation.IN_MINIMAP;
    }

    public static final class Builder {

        private Builder() {}

        public OreVeinElementRenderer build() {
            return new OreVeinElementRenderer(new OreVeinElementReader(), new OreVeinElementRenderProvider(),
                    new OreVeinElementContext());
        }

        public static OreVeinElementRenderer.Builder begin() {
            return new OreVeinElementRenderer.Builder();
        }
    }
}
