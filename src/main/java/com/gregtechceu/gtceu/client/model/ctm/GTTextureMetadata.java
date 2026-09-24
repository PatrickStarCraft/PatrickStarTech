package com.gregtechceu.gtceu.client.model.ctm;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.utils.TriState;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

public record GTTextureMetadata(@Nullable Identifier connectionTexture, TriState bloom) {

    public static final String SECTION_NAME = GTCEu.MOD_ID;
    private static final Identifier EMPTY_CONNECTION = Identifier.fromNamespaceAndPath("", "");
    public static final Codec<GTTextureMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("connection_texture", EMPTY_CONNECTION).forGetter(GTTextureMetadata::connectionTexture),
            TriState.CODEC.optionalFieldOf("bloom", TriState.DEFAULT).forGetter(GTTextureMetadata::bloom)
    ).apply(instance, GTTextureMetadata::new));
    public static final MetadataSectionType<GTTextureMetadata> SERIALIZER = new MetadataSectionType<>(SECTION_NAME, CODEC);

    public static final GTTextureMetadata EMPTY = new GTTextureMetadata(null, TriState.DEFAULT);

    /**
     * @apiNote This method throws {@link IOException} even though it isn't specified in the method definition.
     */
    @SneakyThrows(IOException.class)
    public static Optional<GTTextureMetadata> getForResourceUnsafe(Resource resource) {
        return resource.metadata().getSection(SERIALIZER);
    }

    public GTTextureMetadata {
        // Optional codec fields can't have null as the default value, so we do this instead.
        // It's impossible to define an entirely empty Identifier in a resource file,
        // as even ":" is converted to "minecraft:". Thus, this should be entirely safe.
        if (connectionTexture == EMPTY_CONNECTION) connectionTexture = null;
    }
}
