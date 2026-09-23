package com.gregtechceu.gtceu.data.tags;

import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import com.tterrag.registrate.providers.RegistrateTagsProvider;

public class EntityTypeTagLoader {

    public static void init(RegistrateTagsProvider.Impl<EntityType<?>> provider) {
        provider.tag(CustomTags.HEAT_IMMUNE)
                .add(key("blaze"), key("magma_cube"))
                .add(key("wither_skeleton"), key("wither"));
        provider.tag(CustomTags.CHEMICAL_IMMUNE)
                .add(key("skeleton"), key("stray"));
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(path));
    }
}
