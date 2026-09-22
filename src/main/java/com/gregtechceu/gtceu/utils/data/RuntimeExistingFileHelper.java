package com.gregtechceu.gtceu.utils.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.data.model.builder.ModelFileHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Resource lookup against the game's resource managers. The default helper deliberately permits
 * forward references while runtime generators are being populated. Use activeHelper() to test
 * real resources (including outputs already tracked by a generator).
 */
public class RuntimeExistingFileHelper implements ModelFileHelper {

    public static final RuntimeExistingFileHelper INSTANCE = new RuntimeExistingFileHelper(HashMultimap.create());
    protected final Multimap<PackType, Identifier> generated;
    private Active activeHelper;

    protected RuntimeExistingFileHelper(Multimap<PackType, Identifier> generated) {
        this.generated = generated;
    }

    public static ResourceManager getManager(PackType packType) {
        if (packType == PackType.CLIENT_RESOURCES) return Minecraft.getInstance().getResourceManager();
        if (packType == PackType.SERVER_DATA) {
            var server = GTCEu.getMinecraftServer();
            if (server == null) throw new IllegalStateException("Cannot get server resources without a server.");
            return server.getResourceManager();
        }
        throw new IllegalArgumentException("Invalid pack type " + packType);
    }

    protected Identifier getLocation(Identifier base, String prefix, String suffix) {
        return base.withPath(path -> prefix + "/" + path + suffix);
    }

    private Identifier getLocation(Identifier base, ResourceType type) {
        return type == ResourceType.MODEL ? getLocation(base, "models", ".json") : getLocation(base, "textures", ".png");
    }

    public Active activeHelper() {
        if (activeHelper == null) activeHelper = new Active(generated);
        return activeHelper;
    }

    @Override
    public boolean exists(Identifier loc, ResourceType type) {
        return exists(getLocation(loc, type), PackType.CLIENT_RESOURCES);
    }

    public boolean exists(Identifier loc, PackType packType) { return true; }

    @Override
    public void trackGenerated(Identifier loc, ResourceType type) {
        generated.put(PackType.CLIENT_RESOURCES, getLocation(loc, type));
    }

    public void trackGenerated(Identifier loc, PackType packType, String suffix, String prefix) {
        generated.put(packType, getLocation(loc, prefix, suffix));
    }

    public Resource getResource(Identifier loc, PackType packType,
                                String pathSuffix, String pathPrefix) throws FileNotFoundException {
        return getResource(getLocation(loc, pathPrefix, pathSuffix), packType);
    }

    public Resource getResource(Identifier loc, PackType packType) throws FileNotFoundException {
        return getManager(packType).getResourceOrThrow(loc);
    }

    public List<Resource> getResourceStack(Identifier loc, PackType packType) {
        return getManager(packType).getResourceStack(loc);
    }

    /** Checking is scoped to this helper instance; closing does not close the game's resource manager. */
    public static final class Active extends RuntimeExistingFileHelper implements AutoCloseable {

        private Active(Multimap<PackType, Identifier> generated) { super(generated); }
        @Override
        public Active activeHelper() { return this; }
        @Override
        public boolean exists(Identifier loc, PackType packType) {
            return generated.containsEntry(packType, loc) || getManager(packType).getResource(loc).isPresent();
        }
        @Override
        public void close() {}
    }
}
