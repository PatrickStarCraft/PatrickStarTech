package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.util.random.Weighted;

import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(MultiVariant.class)
public class MultiVariantMixin {

    /**
     * @author RubenVerg
     * @reason Convert variants into the target block-state model hierarchy.
     */
    @Overwrite
    public BlockStateModel.Unbaked toUnbaked() {
        MultiVariant multiVariant = (MultiVariant) (Object) this;
        if (!multiVariant.customBlockStateModels().isEmpty()) {
            var builders = multiVariant.customBlockStateModels().unwrap();
            if (builders.size() == 1) {
                return builders.getFirst().value().toUnbaked();
            }
            return new WeightedVariants.Unbaked(multiVariant.customBlockStateModels()
                    .map(CustomBlockStateModelBuilder::toUnbaked));
        }

        List<Weighted<Variant>> entries = multiVariant.variants().unwrap();
        if (entries.size() == 1) {
            return new SingleVariant.Unbaked(entries.getFirst().value());
        }
        return new WeightedVariants.Unbaked(
                multiVariant.variants().map(SingleVariant.Unbaked::new));
    }
}
