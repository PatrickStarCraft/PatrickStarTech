package com.gregtechceu.gtceu.data.pack;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.common.data.GTRecipes;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.GeneratedRecipe;

import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GTDynamicDataPack implements PackResources {

    public static final FileToIdConverter RECIPE_ID_CONVERTER = GeneratedRecipe.RECIPES;
    public static final FileToIdConverter ADVANCEMENT_ID_CONVERTER = GeneratedRecipe.ADVANCEMENTS;
    public static final Function<String, FileToIdConverter> TAG_ID_CONVERTER = Util
            .memoize(registryName -> FileToIdConverter.json("tags/" + registryName));

    protected static final ObjectSet<String> SERVER_DOMAINS = new ObjectOpenHashSet<>();
    protected static final GTDynamicPackContents CONTENTS = new GTDynamicPackContents();

    private final String name;

    static {
        SERVER_DOMAINS.addAll(Sets.newHashSet(GTCEu.MOD_ID, "minecraft", "forge", "c"));
    }

    public GTDynamicDataPack(String name) {
        this(name, AddonFinder.getAddons().stream().map(IGTAddon::addonModId).collect(Collectors.toSet()));
    }

    public GTDynamicDataPack(String name, Collection<String> domains) {
        this.name = name;
        SERVER_DOMAINS.addAll(domains);
    }

    public static void clearServer() {
        CONTENTS.clearData();
    }

    private static void addToData(Identifier location, byte[] bytes) {
        CONTENTS.addToData(location, bytes);
    }

    public static void addRecipe(GeneratedRecipe recipe) {
        JsonObject recipeJson = recipe.recipeJson();
        byte[] recipeBytes = recipeJson.toString().getBytes(StandardCharsets.UTF_8);
        Path parent = GTCEu.GTCEU_FOLDER.resolve("dumped/data");
        Identifier recipeId = recipe.id();
        if (ConfigHolder.INSTANCE.dev.dumpRecipes) {
            writeJson(recipeId, "recipe", parent, recipeBytes);
        }
        addToData(getRecipeLocation(recipeId), recipeBytes);

        JsonObject advancement = recipe.advancementJson();
        if (advancement != null) {
            byte[] advancementBytes = advancement.toString().getBytes(StandardCharsets.UTF_8);
            if (ConfigHolder.INSTANCE.dev.dumpRecipes) {
                writeJson(recipe.advancementId(), "advancement", parent, advancementBytes);
            }
            addToData(getAdvancementLocation(Objects.requireNonNull(recipe.advancementId())),
                    advancementBytes);
        }
    }

    /**
     * if subdir is null, no file ending is appended.
     *
     * @param id     the resource location of the file to be written.
     * @param subdir a nullable subdirectory for the data.
     * @param parent the parent folder where to write data to.
     * @param json   the json to write.
     */
    @ApiStatus.Internal
    public static void writeJson(Identifier id, @Nullable String subdir, Path parent, byte[] json) {
        try {
            Path file;
            if (subdir != null) {
                // assume JSON
                file = parent.resolve(id.getNamespace()).resolve(subdir).resolve(id.getPath() + ".json");
            } else {
                // assume the file type is also appended if a full path is given.
                file = parent.resolve(id.getNamespace()).resolve(id.getPath());
            }
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                output.write(json);
            }
        } catch (IOException e) {
            GTCEu.LOGGER.error("Failed to write JSON export for file {}", id, e);
        }
    }

    public static void addAdvancement(Identifier loc, JsonObject obj) {
        Identifier l = getAdvancementLocation(loc);
        addToData(l, obj.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length > 0 && elements[0].equals("pack.png")) {
            return () -> GTCEu.class.getResourceAsStream("/icon.png");
        }
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        if (type == PackType.SERVER_DATA) {
            return CONTENTS.getResource(location);
        } else {
            return null;
        }
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        if (packType == PackType.SERVER_DATA) {
            CONTENTS.listResources(namespace, path, resourceOutput);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? SERVER_DOMAINS : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> metaReader) {
        if (metaReader == PackMetadataSection.SERVER_TYPE) {
            return (T) new PackMetadataSection(Component.literal("GTCEu dynamic data"),
                    new InclusiveRange<>(SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA)));
        } else if (metaReader.name().equals("filter")) {
            JsonObject filter = new JsonObject();
            JsonArray block = new JsonArray();
            GTRecipes.RECIPE_FILTERS.forEach((id) -> { // Collect removed recipes in here, in the pack filter section.
                JsonObject entry = new JsonObject();
                entry.addProperty("namespace", "^" + id.getNamespace().replaceAll("[\\W]", "\\\\$0") + "$");
                entry.addProperty("path", "^recipes/" + id.getPath().replaceAll("[\\W]", "\\\\$0") + "\\.json" + "$");
                block.add(entry);
            });
            filter.add("block", block);
            return metaReader.codec().parse(JsonOps.INSTANCE, filter).result().orElse(null);
        }
        return null;
    }

    @Override
    public @NotNull String packId() {
        return this.name;
    }

    @Override
    public PackLocationInfo location() {
        return new PackLocationInfo(this.name, Component.literal(this.name), PackSource.BUILT_IN,
                Optional.empty());
    }

    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
        // NOOP
    }

    public static Identifier getRecipeLocation(Identifier recipeId) {
        return RECIPE_ID_CONVERTER.idToFile(recipeId);
    }

    public static Identifier getAdvancementLocation(Identifier advancementId) {
        return ADVANCEMENT_ID_CONVERTER.idToFile(advancementId);
    }

    public static Identifier getTagLocation(String identifier, Identifier tagId) {
        return TAG_ID_CONVERTER.apply(identifier).idToFile(tagId);
    }
}
