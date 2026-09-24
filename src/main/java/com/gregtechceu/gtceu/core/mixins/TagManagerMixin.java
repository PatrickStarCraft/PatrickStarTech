package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.core.IGTTagLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TagLoader.class)
public class TagManagerMixin {

    @ModifyExpressionValue(method = "loadPendingTags", at = @At(value = "NEW", target = "net/minecraft/tags/TagLoader"))
    private static <T> TagLoader<Holder<T>> gtceu$saveTagLoaderRegistry(TagLoader<Holder<T>> loader,
                                                                        ResourceManager manager, Registry<T> registry) {
        ((IGTTagLoader) loader).gtceu$setRegistry(registry);
        return loader;
    }
}
