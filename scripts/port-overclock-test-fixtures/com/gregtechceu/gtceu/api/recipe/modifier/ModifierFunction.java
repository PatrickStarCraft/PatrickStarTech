package com.gregtechceu.gtceu.api.recipe.modifier;

import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

/** Compile-only modifier contract for the unexecuted getModifier and OCResult.toModifier paths. */
public final class ModifierFunction {
    public static final ModifierFunction IDENTITY = new ModifierFunction();

    private ModifierFunction() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        public Builder modifyAllContents(ContentModifier modifier) { return this; }
        public Builder eutMultiplier(double multiplier) { return this; }
        public Builder durationMultiplier(double multiplier) { return this; }
        public Builder addOCs(int amount) { return this; }
        public Builder subtickParallels(int amount) { return this; }
        public ModifierFunction build() { return IDENTITY; }
    }
}
