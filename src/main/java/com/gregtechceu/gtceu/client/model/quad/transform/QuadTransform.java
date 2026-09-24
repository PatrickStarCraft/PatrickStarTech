package com.gregtechceu.gtceu.client.model.quad.transform;

import com.gregtechceu.gtceu.client.model.quad.MutableQuadView;

import net.minecraft.client.resources.model.geometry.BakedQuad;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface QuadTransform {

    /**
     * Return false to filter out quads from rendering. When more than one transform is in effect, returning false
     * means unapplied transforms will not receive the quad.
     */
    boolean transform(MutableQuadView quad);

    /**
     * Applies this transform to an immutable target quad, returning a new quad or null when the transform filters it.
     */
    default @Nullable BakedQuad applyTo(BakedQuad quad) {
        MutableQuadView view = MutableQuadView.getInstance().fromVanilla(quad, null);
        if (!this.transform(view)) {
            view.clear();
            return null;
        }
        BakedQuad transformed = view.toBakedQuad(quad.materialInfo().sprite());
        view.clear();
        return transformed;
    }
}
