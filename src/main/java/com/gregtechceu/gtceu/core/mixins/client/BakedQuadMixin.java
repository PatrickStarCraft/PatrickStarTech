package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.core.util.extensions.BakedQuadExt;

import net.minecraft.client.resources.model.geometry.BakedQuad;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Mixin(BakedQuad.class)
public class BakedQuadMixin implements BakedQuadExt {

    @Unique
    private String gtceu$textureKey = null;

    @Override
    public BakedQuad gtceu$setTextureKey(@Nullable String key) {
        this.gtceu$textureKey = key;
        return (BakedQuad) (Object) this;
    }

    @Override
    public @Nullable String gtceu$getTextureKey() {
        return gtceu$textureKey;
    }
}
