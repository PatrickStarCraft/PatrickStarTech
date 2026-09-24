package com.gregtechceu.gtceu.api.registry.registrate;

import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.GeneratorType;
import com.tterrag.registrate.providers.*;
import com.tterrag.registrate.providers.generators.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.nullness.*;

import java.util.List;
import java.util.function.Supplier;

public class GTBlockBuilder<T extends Block, P> extends BlockBuilder<T, P> {

    // spotless:off
    public static <T extends Block, P> GTBlockBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name,
                                                                   BuilderCallback callback,
                                                                   NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return new GTBlockBuilder<>(owner, parent, name, callback, factory, BlockBehaviour.Properties::of)
                .defaultBlockstate().defaultLoot().defaultLang();
    }

    protected GTBlockBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback,
                             NonNullFunction<BlockBehaviour.Properties, T> factory,
                             NonNullSupplier<BlockBehaviour.Properties> initialProperties) {
        super(owner, parent, name, callback, factory, initialProperties);
    }

    public GTBlockBuilder<T, P> exBlockstate(NonNullBiConsumer<DataGenContext<Block, ? extends Block>, GTBlockstateProvider> cons) {
        return setDataGeneric(GTBlockstateProvider.BLOCKSTATE, cons);
    }

    public GTBlockBuilder<T, P> gtBlockstate(NonNullBiConsumer<DataGenContext<Block, T>, GTBlockstateProvider> cons) {
        getOwner().setDataGenerator(this, GTBlockstateProvider.BLOCKSTATE,
                prov -> cons.accept(DataGenContext.from(this), prov));
        return this;
    }

    // region default overrides

    @Override
    public GTBlockBuilder<T, P> properties(NonNullUnaryOperator<BlockBehaviour.Properties> func) {
        return (GTBlockBuilder<T, P>) super.properties(func);
    }

    @Override
    public GTBlockBuilder<T, P> initialProperties(NonNullSupplier<? extends Block> block) {
        return (GTBlockBuilder<T, P>) super.initialProperties(block);
    }

    @Override
    public GTBlockBuilder<T, P> simpleItem() {
        return (GTBlockBuilder<T, P>) super.simpleItem();
    }

    @Override
    public <BE extends BlockEntity> GTBlockBuilder<T, P> simpleBlockEntity(BlockEntityBuilder.BlockEntityFactory<BE> factory) {
        return (GTBlockBuilder<T, P>) super.simpleBlockEntity(factory);
    }

    @Override
    public <BE extends BlockEntity> BlockEntityBuilder<BE, BlockBuilder<T, P>> blockEntity(BlockEntityBuilder.BlockEntityFactory<BE> factory) {
        return super.blockEntity(factory);
    }

    @Override
    public GTBlockBuilder<T, P> color(NonNullSupplier<Supplier<List<BlockTintSource>>> tintSources) {
        return (GTBlockBuilder<T, P>) super.color(tintSources);
    }

    @Override
    public GTBlockBuilder<T, P> defaultBlockstate() {
        return (GTBlockBuilder<T, P>) super.defaultBlockstate();
    }

    @Override
    public GTBlockBuilder<T, P> blockstate(NonNullSupplier<NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockModelGenerator>> cons) {
        return (GTBlockBuilder<T, P>) super.blockstate(cons);
    }

    @Override
    public GTBlockBuilder<T, P> defaultLang() {
        return (GTBlockBuilder<T, P>) super.defaultLang();
    }

    @Override
    public GTBlockBuilder<T, P> lang(String name) {
        return (GTBlockBuilder<T, P>) super.lang(name);
    }

    @Override
    public GTBlockBuilder<T, P> defaultLoot() {
        return (GTBlockBuilder<T, P>) super.defaultLoot();
    }

    @Override
    public GTBlockBuilder<T, P> loot(NonNullBiConsumer<RegistrateBlockLootTables, T> cons) {
        return (GTBlockBuilder<T, P>) super.loot(cons);
    }

    @Override
    public GTBlockBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Block, T>, RegistrateRecipeProvider> cons) {
        return (GTBlockBuilder<T, P>) super.recipe(cons);
    }

    // why is it final >:(
    // @SafeVarargs
    // public final BlockBuilder<T, P> tag(TagKey<Block>... tags) {
    //     return tag(ProviderType.BLOCK_TAGS, tags);
    // }

    public <D> GTBlockBuilder<T, P> setDataGeneric(GeneratorType<? extends D> type, NonNullBiConsumer<DataGenContext<Block, ? extends Block>, D> cons) {
        getOwner().setDataGenerator(this, type, prov -> cons.accept(DataGenContext.from(this), prov));
        return this;
    }

    @Override
    public <D> GTBlockBuilder<T, P> setData(GeneratorType<? extends D> type, NonNullBiConsumer<DataGenContext<Block, T>, D> cons) {
        super.setData(type, cons);
        return this;
    }

    // spotless:on
    // endregion
}
