package com.gregtechceu.gtceu.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.registry.registrate.SoundEntryBuilder;
import com.gregtechceu.gtceu.common.data.GTDamageTypes;
import com.gregtechceu.gtceu.common.data.worldgen.*;
import com.gregtechceu.gtceu.data.loot.GTLootModifications;
import com.gregtechceu.gtceu.data.loot.GTLootTables;
import com.gregtechceu.gtceu.data.tags.BiomeTagsLoader;
import com.gregtechceu.gtceu.data.tags.DamageTagsLoader;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;

@net.neoforged.fml.common.EventBusSubscriber()
public class DataGenerators {

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        event.addProvider(new SoundEntryBuilder.SoundEntryProvider(packOutput, GTCEu.MOD_ID));
    }

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        var registries = event.getLookupProvider();
        var set = Set.of(GTCEu.MOD_ID);
        event.addProvider(new BiomeTagsLoader(packOutput, registries));
        DatapackBuiltinEntriesProvider provider = event.addProvider(new DatapackBuiltinEntriesProvider(
                packOutput, registries, new RegistrySetBuilder()
                        .add(Registries.DAMAGE_TYPE, GTDamageTypes::bootstrap)
                        .add(Registries.CONFIGURED_FEATURE, GTConfiguredFeatures::bootstrap)
                        .add(Registries.PLACED_FEATURE, GTPlacedFeatures::bootstrap)
                        .add(Registries.DENSITY_FUNCTION, GTDensityFunctions::bootstrap)
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GTBiomeModifiers::bootstrap),
                set));
        event.addProvider(new DamageTagsLoader(packOutput, provider.getRegistryProvider()));
        event.addProvider(new GTLootTables(packOutput, registries));
        event.addProvider(new GTLootModifications(packOutput, registries));
    }
}
