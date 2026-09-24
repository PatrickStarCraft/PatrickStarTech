package com.gregtechceu.gtceu.client.model.item;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Immutable cover snapshot consumed by facade geometry during chunk meshing. */
public record FacadeRenderState(Map<Direction, Facade> facades, double coverPlateThickness,
                                Set<Direction> occupiedFaces) {

    public static final FacadeRenderState EMPTY = new FacadeRenderState(Map.of(), 0.0, Set.of());

    public FacadeRenderState(Map<Direction, Facade> facades) {
        this(facades, 0.0, facades.keySet());
    }

    public FacadeRenderState {
        EnumMap<Direction, Facade> copy = new EnumMap<>(Direction.class);
        copy.putAll(facades);
        facades = Map.copyOf(copy);
        occupiedFaces = Set.copyOf(occupiedFaces);
    }

    public record Facade(BlockState state, boolean shouldRenderPlate, boolean shouldRenderBackSide) {}
}
