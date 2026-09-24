package com.gregtechceu.gtceu.api.recipe.content;

/** Compile-only modifier contract for the unexecuted OCResult.toModifier path. */
public final class ContentModifier {
    private ContentModifier() {}

    public static ContentModifier multiplier(int amount) {
        return new ContentModifier();
    }
}
