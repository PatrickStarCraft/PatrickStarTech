package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.core.Direction;

/** Vanilla-style element UV calculation for generated block model faces. */
public final class BlockModelUV {

    private BlockModelUV() {}

    /** Computes the default face UVs for the supplied element bounds. */
    public static float[] face(Direction face, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        float u1, v1, u2, v2;
        switch (face) {
            case DOWN -> { u1 = x1; v1 = 16.0f - z2; u2 = x2; v2 = 16.0f - z1; }
            case UP -> { u1 = x1; v1 = z1; u2 = x2; v2 = z2; }
            case NORTH -> { u1 = 16.0f - x2; v1 = 16.0f - y2; u2 = 16.0f - x1; v2 = 16.0f - y1; }
            case SOUTH -> { u1 = x1; v1 = 16.0f - y2; u2 = x2; v2 = 16.0f - y1; }
            case WEST -> { u1 = z1; v1 = 16.0f - y2; u2 = z2; v2 = 16.0f - y1; }
            case EAST -> { u1 = 16.0f - z2; v1 = 16.0f - y2; u2 = 16.0f - z1; v2 = 16.0f - y1; }
            default -> throw new IllegalArgumentException("Unsupported model face: " + face);
        }
        return new float[]{u1, v1, u2, v2};
    }

    /**
     * Insets UVs that touch the texture border. 26.2's transparency scan reads one pixel around a face UV
     * rectangle, so fully opaque models tolerate 0..16 while alpha-tested sprites require a one-pixel margin.
     */
    public static float[] insetTextureEdges(float[] uv, float inset) {
        if (inset <= 0.0f) return uv;
        float maximum = 16.0f - inset;
        return new float[]{
                Math.max(inset, Math.min(maximum, uv[0])),
                Math.max(inset, Math.min(maximum, uv[1])),
                Math.max(inset, Math.min(maximum, uv[2])),
                Math.max(inset, Math.min(maximum, uv[3]))
        };
    }
}
