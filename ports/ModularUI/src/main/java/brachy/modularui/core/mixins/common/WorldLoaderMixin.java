package brachy.modularui.core.mixins.common;

import brachy.modularui.utils.RegistryAccessContainer;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {

    @Inject(method = "lambda$load$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;",
                    shift = At.Shift.BEFORE))
    private static void mui$captureRegistries1(CallbackInfoReturnable<?> cir,
                                               @Local(ordinal = 0) RegistryAccess.Frozen worldgenLoadContext) {
        RegistryAccessContainer.update(worldgenLoadContext, null);
    }

    @Inject(method = "lambda$load$2", at = @At("HEAD"))
    private static void mui$captureRegistries2(CallbackInfoReturnable<?> cir,
                                               @Local(ordinal = 0) LayeredRegistryAccess<RegistryLayer> initialLayers,
                                               @Local(ordinal = 0) RegistryAccess.Frozen loadedWorldgenRegistries,
                                               @Local(ordinal = 1) RegistryAccess.Frozen initialWorldgenDimensions) {
        RegistryAccess.Frozen registriesWithEverything = initialLayers.replaceFrom(
                RegistryLayer.WORLDGEN, loadedWorldgenRegistries, initialWorldgenDimensions).compositeAccess();
        RegistryAccessContainer.update(registriesWithEverything, null);
    }
}
