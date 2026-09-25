/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gregtechceu.gtceu.client.model.quad;

import com.gregtechceu.gtceu.core.util.extensions.BakedQuadExt;
import com.gregtechceu.gtceu.client.util.quad.GeometryHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import com.mojang.blaze3d.platform.Transparency;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static com.gregtechceu.gtceu.client.model.quad.EncodingFormat.*;

/**
 * Interface for reading quad data encoded by {@link MeshBuilder}.
 * Enables models to do analysis, re-texturing or translation
 * without knowing the renderer's vertex formats and without retaining redundant information.
 *
 * @implNote Base class for all quads / quad makers. Handles the ugly bits of maintaining and encoding the quad state.
 */
@Accessors(fluent = true, chain = true)
public class QuadView {

    /**
     * See {@link MutableQuadView#nominalFace(Direction)}.
     */
    @Getter
    protected @Nullable Direction nominalFace;
    /** True when geometry flags or light face may not match geometry. */
    protected boolean isGeometryInvalid = true;
    protected final Vector3f faceNormal = new Vector3f();
    /**
     * Equivalent to {@link BakedQuad#isShade()}. If false, quad should not have shadows cast on it.
     *
     * @see MutableQuadView#shade(boolean)
     */
    @Getter
    protected boolean shade = true;
    /**
     * Equivalent to {@link BakedQuad#hasAmbientOcclusion()}. If false, quad should not have AO.
     *
     * @see MutableQuadView#ambientOcclusion(boolean)
     */
    @Getter
    protected boolean ambientOcclusion = true;
    /**
     * The quad color index serialized with the quad.
     */
    @Getter
    protected int tintIndex;

    /** Target material metadata is kept beside CPU mesh vertices, not encoded as a GPU vertex layout. */
    protected @Nullable BakedQuad.MaterialInfo materialInfo;
    protected @Nullable String textureKey;

    protected long headerFlags = 0;
    /**
     * Size and where it comes from will vary in subtypes. But in all cases quad is fully encoded to array.
     *
     * -- GETTER --
     * Reference to underlying array. Use with caution. Meant for fast renderer access
     */
    @Getter
    protected int @UnknownNullability [] data;

    /** Beginning of the quad. Also the header index. */
    protected int baseIndex = 0;

    /**
     * Use when subtype is "attached" to a pre-existing array. Sets data reference and index and decodes state from
     * array.
     */
    final void load(int[] data, int baseIndex, Mesh.Metadata metadata) {
        this.data = data;
        this.baseIndex = baseIndex;
        this.headerFlags = metadata.headerFlags();
        this.nominalFace = metadata.nominalFace();
        this.shade = metadata.shade();
        this.ambientOcclusion = metadata.ambientOcclusion();
        this.tintIndex = metadata.tintIndex();
        this.materialInfo = metadata.materialInfo();
        this.textureKey = metadata.textureKey();
        load();
    }

    /**
     * Like {@link #load(int[], int)} but assumes array and index already set. Only does the decoding part.
     */
    public final void load() {
        isGeometryInvalid = false;

        // The derived normal is cheap to recover; target material and face metadata are loaded beside the packed data.
        GeometryHelper.computeFaceNormal(faceNormal, this);
    }

    public int normalFlags() {
        return EncodingFormat.normalFlags(headerFlags);
    }

    /** True if any vertex normal has been set. */
    public boolean hasVertexNormals() {
        return normalFlags() != 0;
    }

    @ApiStatus.Internal
    public void computeGeometry() {
        if (isGeometryInvalid) {
            isGeometryInvalid = false;

            GeometryHelper.computeFaceNormal(faceNormal, this);

            // depends on face normal
            headerFlags = EncodingFormat.lightFace(headerFlags, GeometryHelper.lightFace(this));
        }
    }

    /**
     * Equivalent to {@link BakedQuad#getDirection()}. This is the face used for vanilla lighting calculations and will
     * be the block face to which the quad is most closely aligned.
     * Always the same as cull face for quads that are on a block face, but never null.
     */
    public final Direction lightFace() {
        computeGeometry();
        return EncodingFormat.lightFace(headerFlags);
    }

    /**
     * If non-null, quad should not be rendered in-world if the opposite face of a neighbor block occludes it.
     *
     * @see MutableQuadView#cullFace(Direction)
     */
    public final @Nullable Direction cullFace() {
        computeGeometry();
        return EncodingFormat.cullFace(headerFlags);
    }

    /**
     * Normal of the quad as implied by geometry. Will be invalid if quad vertices are not co-planar. Typically computed
     * lazily on demand and not encoded.
     *
     * <p>
     * Not typically needed by models. Exposed to enable standard lighting utility functions for use by renderers.
     */
    public final Vector3f faceNormal() {
        computeGeometry();
        return faceNormal;
    }

    /**
     * Extracts all quad properties except material to the given {@link MutableQuadView} instance. Must be used before
     * calling {link QuadEmitter#emit()} on the target instance. Meant for re-texturing, analysis and static
     * transformation use cases.
     */
    @Contract(mutates = "param")
    public void copyTo(MutableQuadView quad) {
        computeGeometry();

        // copy everything except the material
        System.arraycopy(data, baseIndex, quad.data, quad.baseIndex, QUAD_STRIDE);
        quad.faceNormal.set(faceNormal.x(), faceNormal.y(), faceNormal.z());
        quad.nominalFace = this.nominalFace;
        quad.isGeometryInvalid = false;
        quad.shade(this.shade);
        quad.ambientOcclusion(this.ambientOcclusion);
        quad.tintIndex(this.tintIndex);
        quad.headerFlags = this.headerFlags;
        quad.materialInfo = null;
        quad.textureKey = null;
    }

    /**
     * Returns the specified vertex's geometric position.
     *
     * <p>
     * Pass a non-null target to avoid allocation - will be returned with values. Otherwise, returns a new instance.
     */
    @Contract(value = "_, !null -> param2; _, null -> new", mutates = "param2")
    public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
        if (target == null) {
            target = new Vector3f();
        }

        final int index = baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]),
                Float.intBitsToFloat(data[index + 2]));
        return target;
    }

    /**
     * Returns the specified vertex's geometric position.
     */
    @Contract(value = "_ -> new", pure = true)
    public Vector3f copyPos(int vertexIndex) {
        return copyPos(vertexIndex, null);
    }

    /**
     * Returns the specified vertex's normal vector.
     *
     * <p>
     * Pass a non-null target to avoid allocation - will be returned with values. Otherwise, returns a new instance.
     *
     * <p>
     * Returns null if normal not present.
     */
    @Contract(value = "_, !null -> param2; _, null -> new", mutates = "param2")
    public @Nullable Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
        if (hasNormal(vertexIndex)) {
            if (target == null) {
                target = new Vector3f();
            }

            final int normal = data[normalIndex(vertexIndex)];
            target.set(GeometryHelper.getPackedNormalComponent(normal, 0),
                    GeometryHelper.getPackedNormalComponent(normal, 1),
                    GeometryHelper.getPackedNormalComponent(normal, 2));
            return target;
        } else {
            return null;
        }
    }

    /**
     * Returns the specified vertex's normal vector.
     *
     * <p>
     * Returns null if normal not present.
     */
    @Contract(value = "_ -> new", pure = true)
    public @Nullable Vector3f copyNormal(int vertexIndex) {
        return copyNormal(vertexIndex, null);
    }

    /**
     * Returns the specified vertex's UV coordinates.
     *
     * <p>
     * Pass a non-null target to avoid allocation - will be returned with values. Otherwise, returns a new instance.
     */
    @Contract(value = "_, !null -> param2; _, null -> new", mutates = "param2")
    public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
        if (target == null) {
            target = new Vector2f();
        }

        target.set(u(vertexIndex), v(vertexIndex));
        return target;
    }

    /**
     * Returns the specified vertex's UV coordinates.
     */
    @Contract(value = "_ -> new", pure = true)
    public Vector2f copyUv(int vertexIndex) {
        return copyUv(vertexIndex, null);
    }

    /**
     * Retrieve geometric position, x coordinate.
     */
    public float x(int vertexIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X]);
    }

    /**
     * Retrieve geometric position, y coordinate.
     */
    public float y(int vertexIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_Y]);
    }

    /**
     * Retrieve geometric position, z coordinate.
     */
    public float z(int vertexIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_Z]);
    }

    /**
     * Retrieve vertex color.
     */
    public int color(int vertexIndex) {
        return data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR];
    }

    /**
     * Convenience: access x, y, z by index 0-2.
     */
    public float posByIndex(int vertexIndex, int coordinateIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X + coordinateIndex]);
    }

    /**
     * If false, no vertex normal was provided. Lighting should use face normal in that case.
     */
    public boolean hasNormal(int vertexIndex) {
        return (normalFlags() & (1 << vertexIndex)) != 0;
    }

    protected final int normalIndex(int vertexIndex) {
        return baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL;
    }

    /**
     * Will return {@link Float#NaN} if normal not present.
     */
    public float normalX(int vertexIndex) {
        return hasNormal(vertexIndex) ? GeometryHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 0) :
                Float.NaN;
    }

    /**
     * Will return {@link Float#NaN} if normal not present.
     */
    public float normalY(int vertexIndex) {
        return hasNormal(vertexIndex) ? GeometryHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 1) :
                Float.NaN;
    }

    /**
     * Will return {@link Float#NaN} if normal not present.
     */
    public float normalZ(int vertexIndex) {
        return hasNormal(vertexIndex) ? GeometryHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 2) :
                Float.NaN;
    }

    /**
     * Retrieve horizontal texture coordinates.
     */
    public float u(int vertexIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U]);
    }

    /**
     * Retrieve vertical texture coordinates.
     */
    public float v(int vertexIndex) {
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_V]);
    }

    /** Generates an immutable target-version quad using the separate position, UV, color and normal fields. */
    public BakedQuad toBakedQuad(TextureAtlasSprite sprite) {
        BakedQuad.MaterialInfo outputMaterial;
        if (materialInfo != null) {
            outputMaterial = new BakedQuad.MaterialInfo(sprite, materialInfo.layer(), materialInfo.itemRenderType(),
                    tintIndex(), shade(), materialInfo.lightEmission(), ambientOcclusion());
        } else {
            Transparency transparency = sprite.contents().computeTransparency(0.0F, 0.0F, 1.0F, 1.0F);
            outputMaterial = BakedQuad.MaterialInfo.of(new Material.Baked(sprite, false), transparency,
                    tintIndex(), shade(), 0, ambientOcclusion());
        }

        BakedNormals normals = hasVertexNormals() ? BakedNormals.of(
                hasNormal(0) ? data[normalIndex(0)] : 0,
                hasNormal(1) ? data[normalIndex(1)] : 0,
                hasNormal(2) ? data[normalIndex(2)] : 0,
                hasNormal(3) ? data[normalIndex(3)] : 0) : BakedNormals.UNSPECIFIED;
        BakedColors colors = BakedColors.of(color(0), color(1), color(2), color(3));

        BakedQuad quad = new BakedQuad(
                copyPos(0), copyPos(1), copyPos(2), copyPos(3),
                UVPair.pack(u(0), v(0)), UVPair.pack(u(1), v(1)),
                UVPair.pack(u(2), v(2)), UVPair.pack(u(3), v(3)),
                lightFace(), outputMaterial, normals, colors);
        return textureKey == null ? quad : ((BakedQuadExt) (Object) quad).gtceu$setTextureKey(textureKey);
    }

    @SuppressWarnings("deprecation")
    public BakedQuad toBlockBakedQuad() {
        var finder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS));
        return toBakedQuad(finder.find(this));
    }
}
