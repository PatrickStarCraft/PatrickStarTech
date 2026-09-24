package com.gregtechceu.gtceu.integration.map.journeymap;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.integration.map.IWaypointHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;

import java.util.Map;

public class JourneymapWaypointHandler implements IWaypointHandler {

    private static final Map<String, Waypoint> waypoints = new Object2ObjectOpenHashMap<>();

    @Override
    public void setWaypoint(String key, String name, int color, ResourceKey<Level> dim, BlockPos pos) {
        Waypoint waypoint = WaypointFactory.createWaypoint(GTCEu.MOD_ID, pos, name, dim, true);
        waypoint.setColor(color);
        Waypoint previous = waypoints.put(key, waypoint);
        var api = JourneyMapPlugin.getJmApi();
        if (previous != null) api.removeWaypoint(GTCEu.MOD_ID, previous);
        api.addWaypoint(GTCEu.MOD_ID, waypoint);
    }

    @Override
    public void removeWaypoint(String key) {
        Waypoint removed = waypoints.remove(key);
        if (removed != null) {
            JourneyMapPlugin.getJmApi().removeWaypoint(GTCEu.MOD_ID, removed);
        }
    }
}
