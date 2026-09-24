package com.gregtechceu.gtceu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.NotNull;

/**
 * Compatibility wrapper for GUI sprites. ModularUI owns GregTech's GUI textures;
 * vanilla GUI sprites are resolved through the current atlas manager.
 */
public class GuiSpriteManager {

    public static final Identifier LOCATION_GUI = AtlasIds.GUI;

    private static final GuiSpriteManager INSTANCE = new GuiSpriteManager();

    private GuiSpriteManager() {}

    public static GuiSpriteManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a sprite associated with the passed resource location.
     */
    public @NotNull TextureAtlasSprite getSprite(@NotNull Identifier location) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(LOCATION_GUI).getSprite(location);
    }
}
