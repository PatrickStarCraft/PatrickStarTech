package com.gregtechceu.gtceu.client.util.quad.transformers;

import com.mojang.blaze3d.platform.Transparency;

import com.gregtechceu.gtceu.core.util.extensions.BakedQuadExt;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.UnaryOperator;

public final class GTQuadTransformers {

    public static UnaryOperator<BakedQuad> offset(float by) {
        return offset(by, by, by);
    }

    /**
     * Expand each quad corner along the axes just as the old bakery's per-face
     * corner offsets did, returning a fresh immutable 26.2 quad.
     */
    public static UnaryOperator<BakedQuad> offset(float xOffset, float yOffset, float zOffset) {
        if (xOffset == 0.0f && yOffset == 0.0f && zOffset == 0.0f) return UnaryOperator.identity();

        return quad -> {
            Vector3fc[] positions = copyPositions(quad);
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (Vector3fc position : positions) {
                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                minZ = Math.min(minZ, position.z());
                maxX = Math.max(maxX, position.x());
                maxY = Math.max(maxY, position.y());
                maxZ = Math.max(maxZ, position.z());
            }

            Direction direction = quad.direction();
            for (int i = 0; i < positions.length; i++) {
                Vector3fc position = positions[i];
                positions[i] = new Vector3f(
                        position.x() + xOffset * cornerSign(position.x(), minX, maxX, direction.getStepX()),
                        position.y() + yOffset * cornerSign(position.y(), minY, maxY, direction.getStepY()),
                        position.z() + zOffset * cornerSign(position.z(), minZ, maxZ, direction.getStepZ()));
            }
            return copyWith(quad, positions, copyUvs(quad), quad.materialInfo(), computeNormals(positions), quad.bakedColors());
        };
    }

    public static BakedQuad setSprite(BakedQuad quad, TextureAtlasSprite sprite) {
        TextureAtlasSprite oldSprite = quad.materialInfo().sprite();
        long[] uvs = copyUvs(quad);
        for (int i = 0; i < uvs.length; i++) {
            float u = UVPair.unpackU(uvs[i]);
            float v = UVPair.unpackV(uvs[i]);
            u = Mth.map(u, oldSprite.getU0(), oldSprite.getU1(), sprite.getU0(), sprite.getU1());
            v = Mth.map(v, oldSprite.getV0(), oldSprite.getV1(), sprite.getV0(), sprite.getV1());
            uvs[i] = UVPair.pack(u, v);
        }

        BakedQuad.MaterialInfo oldInfo = quad.materialInfo();
        Transparency transparency = sprite.contents().computeTransparency(0.0F, 0.0F, 1.0F, 1.0F);
        BakedQuad.MaterialInfo newInfo = BakedQuad.MaterialInfo.of(
                new Material.Baked(sprite, false), transparency, oldInfo.tintIndex(), oldInfo.shade(),
                oldInfo.lightEmission(), oldInfo.ambientOcclusion());
        return copyWith(quad, copyPositions(quad), uvs, newInfo, quad.bakedColors());
    }

    public static BakedQuad setColor(BakedQuad quad, int argbColor, boolean clearTintIndex) {
        BakedQuad.MaterialInfo oldInfo = quad.materialInfo();
        BakedQuad.MaterialInfo newInfo = new BakedQuad.MaterialInfo(oldInfo.sprite(), oldInfo.layer(),
                oldInfo.itemRenderType(), clearTintIndex ? -1 : oldInfo.tintIndex(), oldInfo.shade(),
                oldInfo.lightEmission(), oldInfo.ambientOcclusion());
        return copyWith(quad, copyPositions(quad), copyUvs(quad), newInfo, new BakedColors.PerQuad(argbColor));
    }

    public static UnaryOperator<BakedQuad> derotate() {
        return quad -> {
            long[] uvs = copyUvs(quad);
            int start = 0;
            float minU = Float.MAX_VALUE;
            float minV = Float.MAX_VALUE;
            for (int i = 0; i < uvs.length; i++) {
                float u = UVPair.unpackU(uvs[i]);
                float v = UVPair.unpackV(uvs[i]);
                if (u <= minU && v <= minV) {
                    minU = Math.min(minU, u);
                    minV = Math.min(minV, v);
                    start = i;
                }
            }
            long[] derotatedUvs = new long[uvs.length];
            for (int i = 0; i < derotatedUvs.length; i++) {
                derotatedUvs[i] = uvs[(i + start) % uvs.length];
            }
            return copyWith(quad, copyPositions(quad), derotatedUvs, quad.materialInfo(), quad.bakedColors());
        };
    }

    public static BakedQuad copy(BakedQuad quad) {
        return copyWith(quad, copyPositions(quad), copyUvs(quad), quad.materialInfo(), quad.bakedColors());
    }

    private static Vector3fc[] copyPositions(BakedQuad quad) {
        return new Vector3fc[] {
                new Vector3f(quad.position0()), new Vector3f(quad.position1()),
                new Vector3f(quad.position2()), new Vector3f(quad.position3())
        };
    }

    private static long[] copyUvs(BakedQuad quad) {
        return new long[] { quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3() };
    }

    private static float cornerSign(float value, float min, float max, int faceStep) {
        if (min == max) return faceStep;
        float midpoint = (min + max) * 0.5F;
        return value < midpoint ? -1.0F : value > midpoint ? 1.0F : 0.0F;
    }

    private static BakedNormals computeNormals(Vector3fc[] positions) {
        float dx0 = positions[3].x() - positions[1].x();
        float dy0 = positions[3].y() - positions[1].y();
        float dz0 = positions[3].z() - positions[1].z();
        float dx1 = positions[2].x() - positions[0].x();
        float dy1 = positions[2].y() - positions[0].y();
        float dz1 = positions[2].z() - positions[0].z();
        float nx = dy1 * dz0 - dz1 * dy0;
        float ny = dz1 * dx0 - dx1 * dz0;
        float nz = dx1 * dy0 - dy1 * dx0;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0F) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        return new BakedNormals.PerQuad(BakedNormals.pack(nx, ny, nz));
    }

    private static BakedQuad copyWith(BakedQuad source, Vector3fc[] positions, long[] uvs,
                                      BakedQuad.MaterialInfo materialInfo, BakedColors colors) {
        return copyWith(source, positions, uvs, materialInfo, source.bakedNormals(), colors);
    }

    private static BakedQuad copyWith(BakedQuad source, Vector3fc[] positions, long[] uvs,
                                      BakedQuad.MaterialInfo materialInfo, BakedNormals normals, BakedColors colors) {
        BakedQuad copy = new BakedQuad(positions[0], positions[1], positions[2], positions[3],
                uvs[0], uvs[1], uvs[2], uvs[3], source.direction(), materialInfo,
                normals, colors);
        return ((BakedQuadExt) (Object) copy).gtceu$setTextureKey(
                ((BakedQuadExt) (Object) source).gtceu$getTextureKey());
    }

    private GTQuadTransformers() {}
}
