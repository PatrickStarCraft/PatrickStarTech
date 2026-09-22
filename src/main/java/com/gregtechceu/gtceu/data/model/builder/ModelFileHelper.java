package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.resources.Identifier;

/** Resolves generated and existing resources while model JSON is being written. */
public interface ModelFileHelper {

    enum ResourceType {
        MODEL,
        TEXTURE
    }

    boolean exists(Identifier id, ResourceType type);

    void trackGenerated(Identifier id, ResourceType type);
}
