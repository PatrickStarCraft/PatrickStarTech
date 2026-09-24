package com.gregtechceu.gtceu.api.registry;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.registry.MaterialRegistry;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.placeholder.Placeholder;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.sound.SoundEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GTRegistries {

    // spotless:off

    // Material related registries

    // spotless:off

    public static final MaterialRegistry MATERIALS = new MaterialRegistry();
    public static final GTRegistry.RL<Element> ELEMENTS = new GTRegistry.RL<>(GTCEu.id("element"));
    public static final GTRegistry.RL<TagPrefix> TAG_PREFIXES = new GTRegistry.RL<>(GTCEu.id("tag_prefix"));
    public static final GTRegistry.RL<MaterialIconSet> MATERIAL_ICON_SETS = new GTRegistry.RL<>(GTCEu.id("material_icon_set"));

    // Recipe related registries

    public static final GTRegistry.RL<GTRecipeType> RECIPE_TYPES = new GTRegistry.RL<>(GTCEu.id("recipe_type"));
    public static final GTRegistry.RL<GTRecipeCategory> RECIPE_CATEGORIES = new GTRegistry.RL<>(GTCEu.id("recipe_category"));
    public static final GTRegistry.RL<RecipeCapability<?>> RECIPE_CAPABILITIES = new GTRegistry.RL<>(GTCEu.id("recipe_capability"));
    public static final GTRegistry.RL<RecipeConditionType<?>> RECIPE_CONDITIONS = new GTRegistry.RL<>(GTCEu.id("recipe_condition"));
    public static final GTRegistry.RL<ChanceLogic> CHANCE_LOGICS = new GTRegistry.RL<>(GTCEu.id("chance_logic"));

    // Worldgen related registries

    public static final GTRegistry.RL<BedrockFluidDefinition> BEDROCK_FLUID_DEFINITIONS = new GTRegistry.RL<>(GTCEu.id("bedrock_fluid"));
    public static final GTRegistry.RL<BedrockOreDefinition> BEDROCK_ORE_DEFINITIONS = new GTRegistry.RL<>(GTCEu.id("bedrock_ore"));
    public static final GTRegistry.RL<GTOreDefinition> ORE_VEINS = new GTRegistry.RL<>(GTCEu.id("ore_vein"));
    public static final GTRegistry.RL<IWorldGenLayer> WORLD_GEN_LAYERS = new GTRegistry.RL<>(GTCEu.id("world_gen_layer"));

    // Other registries

    public static final GTRegistry.RL<CoverDefinition> COVERS = new GTRegistry.RL<>(GTCEu.id("cover"));
    public static final GTRegistry.RL<MachineDefinition> MACHINES = new GTRegistry.RL<>(GTCEu.id("machine"));
    public static final GTRegistry.RL<SoundEntry> SOUNDS = new GTRegistry.RL<>(GTCEu.id("sound"));
    public static final GTRegistry.RL<DimensionMarker> DIMENSION_MARKERS = new GTRegistry.RL<>(GTCEu.id("dimension_marker"));
    public static final GTRegistry.RL<MedicalCondition> MEDICAL_CONDITIONS = new GTRegistry.RL<>(GTCEu.id("medical_condition"));
    public static final GTRegistry.RL<Placeholder> PLACEHOLDERS = new GTRegistry.RL<>(GTCEu.id("placeholder"));
    public static final GTRegistry.RL<PatternError.PatternErrorType> PATTERN_ERRORS = new GTRegistry.RL<>(
            GTCEu.id("pattern_errors"));

    private static final Map<Identifier, RecipeType<?>> PENDING_RECIPE_TYPES = new LinkedHashMap<>();
    private static final Map<Identifier, RecipeSerializer<?>> PENDING_RECIPE_SERIALIZERS = new LinkedHashMap<>();
    private static boolean recipeTypesRegistered;
    private static boolean recipeSerializersRegistered;


    // spotless:on

    public static <V, T extends V> T register(Registry<V> registry, Identifier name, T value) {
        ResourceKey<?> registryKey = registry.key();

        if (registryKey.equals(Registries.RECIPE_TYPE)) {
            queue(PENDING_RECIPE_TYPES, name, (RecipeType<?>) value, recipeTypesRegistered);
        } else if (registryKey.equals(Registries.RECIPE_SERIALIZER)) {
            queue(PENDING_RECIPE_SERIALIZERS, name, (RecipeSerializer<?>) value, recipeSerializersRegistered);
        } else {
            return Registry.register(registry, name, value);
        }

        return value;
    }

    public static void registerRecipeEntries(net.neoforged.neoforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_TYPE)) {
            PENDING_RECIPE_TYPES.forEach((name, recipeType) ->
                    event.register(Registries.RECIPE_TYPE, name, () -> recipeType));
            PENDING_RECIPE_TYPES.clear();
            recipeTypesRegistered = true;
        } else if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            PENDING_RECIPE_SERIALIZERS.forEach((name, serializer) ->
                    event.register(Registries.RECIPE_SERIALIZER, name, () -> serializer));
            PENDING_RECIPE_SERIALIZERS.clear();
            recipeSerializersRegistered = true;
        }
    }

    private static <T> void queue(Map<Identifier, T> pending, Identifier name, T value, boolean alreadyRegistered) {
        if (alreadyRegistered) {
            throw new IllegalStateException("Cannot register " + name + " after its registry event has fired");
        }
        if (pending.putIfAbsent(name, value) != null) {
            throw new IllegalStateException("Duplicate pending registry entry " + name);
        }
    }

    private static final RegistryAccess BLANK = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static RegistryAccess FROZEN = BLANK;

    /**
     * You shouldn't call it, you should probably not even look at it just to be extra safe
     *
     * @param registryAccess the new value to set to the frozen registry access
     */
    @ApiStatus.Internal
    public static void updateFrozenRegistry(RegistryAccess registryAccess) {
        FROZEN = registryAccess;
    }

    public static RegistryAccess builtinRegistry() {
        if (GTCEu.isClientThread()) {
            return ClientHelpers.getClientRegistries();
        }
        return FROZEN;
    }

    private static class ClientHelpers {

        private static RegistryAccess getClientRegistries() {
            if (Minecraft.getInstance().getConnection() != null) {
                return Minecraft.getInstance().getConnection().registryAccess();
            } else {
                return FROZEN;
            }
        }
    }
}
