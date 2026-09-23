package com.gregtechceu.gtceu.data.pack;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;

@RequiredArgsConstructor
public class GTPackSource implements RepositorySource {

    private final String name;
    private final PackType type;
    private final Pack.Position position;
    private final Function<String, PackResources> resources;

    @Override
    public void loadPacks(Consumer<Pack> onLoad) {
        onLoad.accept(readMetaAndCreate(name,
                Component.literal(name),
                true,
                resources,
                type,
                position,
                PackSource.BUILT_IN));
    }

    public static Pack readMetaAndCreate(String id, Component title, boolean required, Function<String, PackResources> resources,
                                         PackType packType, Pack.Position defaultPosition, PackSource packSource) {
        var location = new PackLocationInfo(id, title, packSource, Optional.empty());
        Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo ignored) {
                return resources.apply(id);
            }

            @Override
            public PackResources openFull(PackLocationInfo ignored, Pack.Metadata metadata) {
                return resources.apply(id);
            }
        };
        Pack.Metadata metadata = Pack.readPackMetadata(location, resourcesSupplier,
                SharedConstants.getCurrentVersion().packVersion(packType), packType);
        if (metadata == null) return null;

        var hiddenMetadata = new Pack.Metadata(metadata.description(), metadata.compatibility(),
                metadata.requestedFeatures(), metadata.overlays(), true);
        var selectionConfig = new PackSelectionConfig(required, defaultPosition, false);
        return new Pack(location, resourcesSupplier, hiddenMetadata, selectionConfig);
    }
}
