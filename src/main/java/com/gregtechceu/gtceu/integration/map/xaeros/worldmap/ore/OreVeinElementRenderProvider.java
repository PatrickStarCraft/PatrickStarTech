package com.gregtechceu.gtceu.integration.map.xaeros.worldmap.ore;

import com.gregtechceu.gtceu.integration.map.xaeros.XaerosRenderer;
import com.gregtechceu.gtceu.integration.map.xaeros.common.ore.OreVeinElement;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

import java.util.Iterator;

public class OreVeinElementRenderProvider extends ElementRenderProvider<OreVeinElement, OreVeinElementContext> {

    private Iterator<OreVeinElement> iterator;

    public OreVeinElementRenderProvider() {}

    public void begin(ElementRenderLocation location, OreVeinElementContext context) {
        if (WorldMap.INSTANCE.getConfigs().getClientConfigManager().getEffective(
                WorldMapProfiledConfigOptions.WAYPOINTS)) {
            ResourceKey<Level> currentDim = Minecraft.getInstance().level.dimension();
            this.iterator = XaerosRenderer.oreElements.row(currentDim).values().iterator();

            context.worldmapWaypointsScale = WorldMap.INSTANCE.getConfigs().getClientConfigManager()
                    .getEffective(WorldMapProfiledConfigOptions.WAYPOINT_SCALE).floatValue();
        } else {
            this.iterator = null;
        }
    }

    public boolean hasNext(ElementRenderLocation location, OreVeinElementContext context) {
        return this.iterator != null && this.iterator.hasNext();
    }

    public OreVeinElement getNext(ElementRenderLocation location, OreVeinElementContext context) {
        return this.iterator.next();
    }

    public void end(ElementRenderLocation location, OreVeinElementContext context) {}
}
