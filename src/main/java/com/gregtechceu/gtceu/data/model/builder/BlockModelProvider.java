package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.resources.Identifier;

public class BlockModelProvider extends ModelProvider<BlockModelBuilder> {
    public BlockModelProvider(String namespace, ModelFileHelper helper) { super(namespace, helper); }

    @Override
    protected BlockModelBuilder create(Identifier id) { return new BlockModelBuilder(id, existingFileHelper); }
    @Override
    protected String getFolder() { return "block"; }

    public BlockModelBuilder cross(String name, Identifier texture) {
        return getBuilder(name).parent(new ModelFile.UncheckedModelFile("block/cross"))
                .texture("cross", texture);
    }

    public BlockModelBuilder singleTexture(String name, Identifier parent, String textureKey, Identifier texture) {
        return getBuilder(name).parent(getExistingFile(parent)).texture(textureKey, texture);
    }

    public BlockModelBuilder cubeAll(String name, Identifier texture) {
        return getBuilder(name).parent(new ModelFile.UncheckedModelFile("block/cube_all"))
                .texture("all", texture);
    }

    public BlockModelBuilder cubeBottomTop(String name, Identifier side, Identifier bottom, Identifier top) {
        return getBuilder(name).parent(new ModelFile.UncheckedModelFile("block/cube_bottom_top"))
                .texture("side", side).texture("bottom", bottom).texture("top", top);
    }
}
