package com.gregtechceu.gtceu.client.util;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.bus.api.Event;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AssetEventListener<T extends Event> {

    void accept(T event);

    default @Nullable Class<T> eventClass() {
        return null;
    }

    @FunctionalInterface
    interface AtlasStitched extends AssetEventListener<TextureAtlasStitchedEvent> {

        @Override
        @Nullable
        default Class<TextureAtlasStitchedEvent> eventClass() {
            return TextureAtlasStitchedEvent.class;
        }
    }

    @FunctionalInterface
    interface BlockStateModelReplacement {

        BlockStateModel modifyBlockStateModel(BlockState state, BlockStateModel model);
    }

    @FunctionalInterface
    interface RegisterStandalone extends AssetEventListener<ModelEvent.RegisterStandalone> {

        @Override
        @Nullable
        default Class<ModelEvent.RegisterStandalone> eventClass() {
            return ModelEvent.RegisterStandalone.class;
        }
    }
}
