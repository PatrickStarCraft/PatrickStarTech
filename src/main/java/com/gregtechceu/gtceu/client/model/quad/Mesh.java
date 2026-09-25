package com.gregtechceu.gtceu.client.model.quad;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * A bundle of one or more {@link QuadView} instances encoded by the renderer.
 *
 * <p>
 * Similar in purpose to the {@code List<BakedQuad>} instances returned by BakedModel, but affords the renderer the
 * ability to optimize the format for performance and memory allocation.
 *
 * <p>
 * Only the renderer should implement or extend this interface.
 *
 * @implNote The way we encode meshes makes it very simple.
 */
public class Mesh {

    /** Used to satisfy external calls to {@link #forEach(Consumer)}. */
    private static final ThreadLocal<QuadView> POOL = ThreadLocal.withInitial(QuadView::new);

    final int[] data;
    private final Metadata[] metadata;

    Mesh(int[] data, Metadata[] metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public int[] data() {
        return data;
    }

    public void forEach(Consumer<QuadView> consumer) {
        forEach(consumer, POOL.get());
    }

    /**
     * The renderer will call this with its own cursor to avoid the performance hit of a thread-local lookup. Also
     * means renderer can hold final references to quad buffers.
     */
    void forEach(Consumer<QuadView> consumer, QuadView cursor) {
        final int limit = data.length;
        int index = 0;
        int quadIndex = 0;

        while (index < limit) {
            cursor.load(data, index, metadata[quadIndex++]);
            consumer.accept(cursor);
            index += EncodingFormat.QUAD_STRIDE;
        }
    }

    record Metadata(long headerFlags, @Nullable Direction nominalFace, boolean shade, boolean ambientOcclusion,
                    int tintIndex, @Nullable BakedQuad.MaterialInfo materialInfo, @Nullable String textureKey) {}

    @SuppressWarnings("deprecation")
    public List<BakedQuad> toBlockBakedQuads() {
        SpriteFinder finder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS));

        List<BakedQuad> result = new ArrayList<>();
        forEach(qv -> result.add(qv.toBakedQuad(finder.find(qv))));
        return result;
    }

    @SuppressWarnings("deprecation")
    public void asBlockBakedQuads(Consumer<BakedQuad> consumer) {
        SpriteFinder finder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS));

        forEach(qv -> consumer.accept(qv.toBakedQuad(finder.find(qv))));
    }
}
