package com.gregtechceu.gtceu.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;

import net.minecraft.core.Registry;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.function.Function;

/** Adapts registered values to the registry-key based tag appender used by current data providers. */
final class RegistryTagAppender<T> {

    private final TagAppender<T> appender;
    private final ResourceKey<? extends Registry<T>> registry;
    private final Function<T, ResourceKey<T>> keyOf;

    private RegistryTagAppender(TagAppender<T> appender, ResourceKey<? extends Registry<T>> registry,
                                Function<T, ResourceKey<T>> keyOf) {
        this.appender = appender;
        this.registry = registry;
        this.keyOf = keyOf;
    }

    static <T> RegistryTagAppender<T> create(RegistrateTagsProvider.Impl<T> provider, TagKey<T> tag,
                                              Function<T, ResourceKey<T>> keyOf) {
        return new RegistryTagAppender<>(provider.tag(tag), provider.registry(), keyOf);
    }

    @SafeVarargs
    final RegistryTagAppender<T> add(T... values) {
        for (T value : values) {
            appender.add(keyOf.apply(value));
        }
        return this;
    }

    RegistryTagAppender<T> addOptional(Identifier value) {
        appender.addOptional(ResourceKey.create(registry, value));
        return this;
    }

    RegistryTagAppender<T> addTag(TagKey<T> tag) {
        appender.addTag(tag);
        return this;
    }

    RegistryTagAppender<T> addOptionalTag(Identifier tag) {
        appender.addOptionalTag(TagKey.create(registry, tag));
        return this;
    }

    @SafeVarargs
    final RegistryTagAppender<T> remove(T... values) {
        for (T value : values) {
            appender.remove(keyOf.apply(value));
        }
        return this;
    }
}
