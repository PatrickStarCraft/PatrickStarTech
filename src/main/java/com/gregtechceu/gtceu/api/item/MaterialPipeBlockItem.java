package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;

import org.jspecify.annotations.NullMarked;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@NullMarked
@ParametersAreNonnullByDefault
public class MaterialPipeBlockItem extends PipeBlockItem {

    public MaterialPipeBlockItem(MaterialPipeBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    @NotNull
    public MaterialPipeBlock getBlock() {
        return (MaterialPipeBlock) super.getBlock();
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.getBlock().getName();
    }
}
