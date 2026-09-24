package com.gregtechceu.gtceu.client.model.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Quadrant;

import com.gregtechceu.gtceu.core.util.extensions.BakedQuadExt;

import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.ExtraFaceData;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class StaticFaceBakery {

    public static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);

    public static final AABB SLIGHTLY_OVER_BLOCK = BLOCK.inflate(0.001);
    public static final AABB OUTPUT_OVERLAY = BLOCK.inflate(0.006);
    public static final AABB AUTO_OUTPUT_OVERLAY = BLOCK.inflate(0.008);
    public static final AABB COVER_OVERLAY = BLOCK.inflate(0.002);

    /**
     * This standalone helper is used outside the model-baking lifecycle. It
     * delegates vertex and material construction to the 26.2 bakery without
     * retaining a model baker or its reload-scoped interner.
     */
    private static final ModelBaker.Interner INTERNER = new ModelBaker.Interner() {
        @Override
        public Vector3fc vector(Vector3fc vector) {
            return vector;
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
            return material;
        }
    };

    /**
     * Bake a quad for one cube face.
     *
     * @param cube       cube bounds in block coordinates
     * @param face       face of the quad
     * @param sprite     texture
     * @param cubeUV     derive UV coordinates from cube dimensions
     * @param rotation   model transform
     * @param tintIndex  color tint index
     * @param emissivity light emission from 0 to 15
     * @param cull       whether this face can be culled
     * @param shade      whether this face is shaded
     */
    public static BakedQuad bakeFace(AABB cube, Direction face, TextureAtlasSprite sprite, boolean cubeUV,
                                     ModelState rotation, int tintIndex, int emissivity, boolean cull, boolean shade) {
        Vector3f posFrom = new Vector3f((float) cube.minX * 16f, (float) cube.minY * 16f, (float) cube.minZ * 16f);
        Vector3f posTo = new Vector3f((float) cube.maxX * 16f, (float) cube.maxY * 16f, (float) cube.maxZ * 16f);
        float[] uv;
        if (cubeUV) {
            uv = switch (face) {
                case UP -> new float[] { posFrom.x(), posFrom.z(), posTo.x(), posTo.z() };
                case DOWN -> new float[] { posFrom.x(), posTo.z(), posTo.x(), posFrom.z() };
                case NORTH -> new float[] { posTo.x(), posTo.y(), posFrom.x(), posFrom.y() };
                case SOUTH -> new float[] { posFrom.x(), posTo.y(), posTo.x(), posFrom.y() };
                case WEST -> new float[] { posFrom.z(), posTo.y(), posTo.z(), posFrom.y() };
                case EAST -> new float[] { posTo.z(), posTo.y(), posFrom.z(), posFrom.y() };
            };
        } else {
            uv = new float[] { 0.0F, 0.0F, 16.0F, 16.0F };
        }

        return bakeQuad(posFrom, posTo, uv, sprite, face, rotation, tintIndex, emissivity, cull, shade);
    }

    public static BakedQuad bakeFace(AABB cube, Direction face, TextureAtlasSprite sprite, ModelState rotation,
                                     int tintIndex, int emissivity, boolean cull, boolean shade) {
        return bakeFace(cube, face, sprite, false, rotation, tintIndex, emissivity, cull, shade);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex,
                                     int emissivity, boolean cull, boolean shade) {
        return bakeFace(BLOCK, face, sprite, rotation, tintIndex, emissivity, cull, shade);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation,
                                     int tintIndex, int emissivity) {
        return bakeFace(face, sprite, rotation, tintIndex, emissivity, true, true);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation, int tintIndex) {
        return bakeFace(face, sprite, rotation, tintIndex, 0);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite, ModelState rotation) {
        return bakeFace(face, sprite, rotation, -1);
    }

    public static BakedQuad bakeFace(Direction face, TextureAtlasSprite sprite) {
        return bakeFace(face, sprite, BlockModelRotation.IDENTITY);
    }

    public static BakedQuad bakeFace(AABB cube, Direction face, TextureAtlasSprite sprite, boolean cubeUV) {
        return bakeFace(cube, face, sprite, cubeUV, BlockModelRotation.IDENTITY, -1, 0, true, true);
    }

    public static BakedQuad bakeFace(AABB cube, Direction face, TextureAtlasSprite sprite) {
        return bakeFace(cube, face, sprite, false);
    }

    private static BakedQuad bakeQuad(Vector3fc from, Vector3fc to, float[] uv, TextureAtlasSprite sprite,
                                      Direction facing, ModelState modelState, int tintIndex, int emissivity,
                                      boolean cull, boolean shade) {
        CuboidFace.UVs faceUvs = new CuboidFace.UVs(uv[0], uv[1], uv[2], uv[3]);
        CuboidFace face = new CuboidFace(cull ? facing : null, tintIndex,
                sprite.contents().name().toString(), faceUvs, Quadrant.parseJson(0));
        Material.Baked material = new Material.Baked(sprite, false);
        Transparency transparency = sprite.contents().computeTransparency(
                Math.min(faceUvs.minU(), faceUvs.maxU()) / 16.0F,
                Math.min(faceUvs.minV(), faceUvs.maxV()) / 16.0F,
                Math.max(faceUvs.minU(), faceUvs.maxU()) / 16.0F,
                Math.max(faceUvs.minV(), faceUvs.maxV()) / 16.0F);
        BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(
                material, transparency, tintIndex, shade, emissivity, true);

        BakedQuad quad = FaceBakery.bakeQuad(INTERNER, from, to, faceUvs, face.rotation(), materialInfo,
                facing, modelState, null, ExtraFaceData.DEFAULT);
        return ((BakedQuadExt) (Object) quad).gtceu$setTextureKey(face.texture());
    }
}
