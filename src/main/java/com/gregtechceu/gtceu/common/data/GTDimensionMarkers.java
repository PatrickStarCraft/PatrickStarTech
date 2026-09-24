package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.kjs.GTRegistryInfo;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModLoader;

import com.tterrag.registrate.util.entry.BlockEntry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

@SuppressWarnings("unused")
public class GTDimensionMarkers {

    static {
        GTRegistries.DIMENSION_MARKERS.unfreeze();
        REGISTRATE.creativeModeTab(() -> null);
    }

    public static final BlockEntry<Block> OVERWORLD_MARKER = createMarker("overworld");
    public static final BlockEntry<Block> NETHER_MARKER = createMarker("the_nether");
    public static final BlockEntry<Block> END_MARKER = createMarker("the_end");

    public static final DimensionMarker OVERWORLD = createAndRegister(Level.OVERWORLD.identifier(), 0,
            () -> OVERWORLD_MARKER, null);
    public static final DimensionMarker NETHER = createAndRegister(Level.NETHER.identifier(), 0,
            () -> NETHER_MARKER, null);
    public static final DimensionMarker END = createAndRegister(Level.END.identifier(), 0,
            () -> END_MARKER, null);

    public static DimensionMarker createAndRegister(Identifier dim, int tier, Identifier itemKey,
                                                    @Nullable String overrideName) {
        DimensionMarker marker = new DimensionMarker(tier, itemKey, overrideName);
        marker.register(dim);
        return marker;
    }

    public static DimensionMarker createAndRegister(Identifier dim, int tier, Supplier<ItemLike> supplier,
                                                    @Nullable String overrideName) {
        DimensionMarker marker = new DimensionMarker(tier, supplier, overrideName);
        marker.register(dim);
        return marker;
    }

    private static BlockEntry<Block> createMarker(String name) {
        return REGISTRATE.block("%s_marker".formatted(name), Block::new)
                .lang(FormattingUtil.toEnglishName(name))
                .gtBlockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models()
                        .withExistingParent(ctx.getName(), prov.mcLoc("block/cube"))
                        .texture("down", prov.modLoc("block/dim_markers/%s/down".formatted(name)))
                        .texture("up", prov.modLoc("block/dim_markers/%s/up".formatted(name)))
                        .texture("north", prov.modLoc("block/dim_markers/%s/north".formatted(name)))
                        .texture("south", prov.modLoc("block/dim_markers/%s/south".formatted(name)))
                        .texture("east", prov.modLoc("block/dim_markers/%s/east".formatted(name)))
                        .texture("west", prov.modLoc("block/dim_markers/%s/west".formatted(name)))
                        .texture("particle", "#north")
                        .guiLight(UnbakedModel.GuiLight.FRONT.getSerializedName())))
                .simpleItem()
                .register();
    }

    public static void init() {
        ModLoader.postEvent(new GTCEuAPI.RegisterEvent<>(GTRegistries.DIMENSION_MARKERS, DimensionMarker.class));
        if (GTCEu.Mods.isKubeJSLoaded()) {
            GTRegistryInfo.registerFor(GTRegistries.DIMENSION_MARKERS.getRegistryName());
        }
        GTRegistries.DIMENSION_MARKERS.freeze();
    }
}
