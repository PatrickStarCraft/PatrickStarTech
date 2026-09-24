package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.client.renderer.cover.ICoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.IOCoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;
import com.gregtechceu.gtceu.common.cover.FacadeCover;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** Immutable appearance input for static non-facade cover geometry. */
@OnlyIn(Dist.CLIENT)
public record CoverRenderState(double plateThickness, int occupiedFaces, Map<Direction, CoverFace> faces) {

    public static final CoverRenderState EMPTY = new CoverRenderState(0.0, 0, Map.of());

    public CoverRenderState {
        EnumMap<Direction, CoverFace> copiedFaces = new EnumMap<>(Direction.class);
        copiedFaces.putAll(faces);
        faces = Map.copyOf(copiedFaces);
    }

    /** Captures stable renderer texture IDs and cover configuration while the live cover container is available. */
    public static CoverRenderState capture(ICoverable coverable) {
        EnumMap<Direction, CoverFace> faces = new EnumMap<>(Direction.class);
        int occupiedFaces = 0;

        for (Direction direction : Direction.values()) {
            CoverBehavior cover = coverable.getCoverAtSide(direction);
            if (cover == null) continue;

            occupiedFaces |= 1 << direction.ordinal();
            if (cover instanceof FacadeCover) continue;

            ICoverRenderer renderer = cover.getCoverRenderer().get();
            Identifier overlay = null;
            Identifier emissive = null;
            boolean optionalEmissive = false;
            boolean explicitEmissive = false;
            if (renderer instanceof SimpleCoverRenderer simple) {
                overlay = simple.overlayTexture();
                emissive = simple.emissiveTexture();
                if (emissive == null) {
                    emissive = overlay.withSuffix("_emissive");
                    optionalEmissive = true;
                }
            } else if (renderer instanceof IOCoverRenderer ioRenderer && cover instanceof IIOCover ioCover) {
                boolean inverted = ioCover.getIo() != IO.OUT;
                overlay = inverted && ioRenderer.invertedOverlayTexture() != null
                        ? ioRenderer.invertedOverlayTexture() : ioRenderer.overlayTexture();
                emissive = inverted && ioRenderer.invertedEmissiveOverlayTexture() != null
                        ? ioRenderer.invertedEmissiveOverlayTexture() : ioRenderer.emissiveOverlayTexture();
                explicitEmissive = true;
            }

            if (overlay != null || emissive != null || cover.shouldRenderPlate()) {
                faces.put(direction, new CoverFace(direction, cover.shouldRenderPlate(), overlay, emissive,
                        optionalEmissive, explicitEmissive));
            }
        }

        return new CoverRenderState(coverable.getCoverPlateThickness(), occupiedFaces, faces);
    }

    public record CoverFace(Direction attachedSide, boolean renderPlate, @Nullable Identifier overlay,
                            @Nullable Identifier emissiveOverlay, boolean optionalEmissive,
                            boolean explicitEmissive) {}
}
