package com.gregtechceu.gtceu.client.util;

import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;

import com.google.common.base.Preconditions;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;

/**
 * Precomputes the target renderer's discrete cuboid rotations for machine model variants.
 */
public class VariantRotationHelpers {

    private static final Transformation[] TRANSFORMS = createTransformations();

    private static Transformation[] createTransformations() {
        var result = new Transformation[4 * 4 * 4];

        for (var xRot = 0; xRot < 360; xRot += 90) {
            for (var yRot = 0; yRot < 360; yRot += 90) {
                for (var zRot = 0; zRot < 360; zRot += 90) {
                    var idx = indexFromAngles(xRot, yRot, zRot);
                    var orientation = Quadrant.fromXYZAngles(
                            quadrant(xRot), quadrant(yRot), quadrant(zRot));
                    result[idx] = BlockModelRotation.get(orientation).transformation();
                }
            }
        }

        return result;
    }

    private VariantRotationHelpers() {}

    private static Quadrant quadrant(int degrees) {
        return switch (degrees) {
            case 0 -> Quadrant.R0;
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> throw new IllegalArgumentException("Invalid model rotation " + degrees);
        };
    }

    public static Transformation getRotationTransform(int xRot, int yRot, int zRot) {
        return TRANSFORMS[indexFromAngles(xRot, yRot, zRot)];
    }

    private static int indexFromAngles(int xRot, int yRot, int zRot) {
        Preconditions.checkArgument(xRot >= 0 && xRot < 360 && xRot % 90 == 0);
        Preconditions.checkArgument(yRot >= 0 && yRot < 360 && yRot % 90 == 0);
        Preconditions.checkArgument(zRot >= 0 && zRot < 360 && zRot % 90 == 0);
        return xRot / 90 * 16 + yRot / 90 * 4 + zRot / 90;
    }
}
