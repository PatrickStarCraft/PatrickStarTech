package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.core.MixinHelpers;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.CommonHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {

    @Redirect(
            method = "lambda$scheduleRegistryLoad$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagLoader;loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/WritableRegistry;)V"))
    @SuppressWarnings("unchecked")
    private static void gtceu$registerDynamicLootTables(ResourceManager manager, WritableRegistry<?> registry,
                                                        @Local(argsOnly = true) LootDataType<?> type,
                                                        @Local(argsOnly = true) RegistryOps<JsonElement> ops) {
        if (type == LootDataType.TABLE && !GTCEu.isDataGen()) {
            Map<Identifier, LootTable> dynamicTables = new HashMap<>();
            MixinHelpers.generateGTDynamicLoot(dynamicTables, CommonHooks.extractLookupProvider(ops));

            WritableRegistry<LootTable> lootTables = (WritableRegistry<LootTable>) registry;
            dynamicTables.forEach((id, table) -> {
                ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
                // Generated tables fill gaps while higher-priority data packs keep their overrides.
                if (lootTables.containsKey(key)) return;

                LootDataType.TABLE.idSetter().accept(table, id);
                lootTables.register(key, table,
                        new RegistrationInfo(Optional.empty(), Lifecycle.experimental()));
            });
        }

        // Keep table tags bound after all generated entries have been registered.
        TagLoader.loadTagsForRegistry(manager, registry);
    }
}
