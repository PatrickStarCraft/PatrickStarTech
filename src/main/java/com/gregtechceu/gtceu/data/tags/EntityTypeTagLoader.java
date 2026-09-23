package com.gregtechceu.gtceu.data.tags;

import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.tags.TagKey;

import com.tterrag.registrate.providers.RegistrateTagsProvider;

public class EntityTypeTagLoader {

    public static void init(RegistrateTagsProvider.Impl<EntityType<?>> provider) {
        tag(provider, CustomTags.HEAT_IMMUNE)
                .add(EntityTypes.BLAZE, EntityTypes.MAGMA_CUBE)
                .add(EntityTypes.WITHER_SKELETON, EntityTypes.WITHER);
        tag(provider, CustomTags.CHEMICAL_IMMUNE)
                .add(EntityTypes.SKELETON, EntityTypes.STRAY);
    }

    private static RegistryTagAppender<EntityType<?>> tag(RegistrateTagsProvider.Impl<EntityType<?>> provider,
                                                            TagKey<EntityType<?>> tag) {
        return RegistryTagAppender.create(provider, tag, entityType -> entityType.builtInRegistryHolder().key());
    }
}
