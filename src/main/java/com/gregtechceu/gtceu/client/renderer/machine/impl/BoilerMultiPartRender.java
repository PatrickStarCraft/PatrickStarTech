package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.client.model.machine.ControllerPartModel;
import com.gregtechceu.gtceu.client.model.machine.ControllerPartRenderState;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.common.block.BoilerFireboxType;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.List;
import java.util.function.Supplier;

public class BoilerMultiPartRender extends DynamicRender<MultiblockControllerMachine, BoilerMultiPartRender>
                                   implements ControllerPartModel {

    // spotless:off
    public static final MapCodec<BoilerMultiPartRender> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("firebox_idle").forGetter(BoilerMultiPartRender::getFireboxIdle),
            BlockState.CODEC.fieldOf("firebox_active").forGetter(BoilerMultiPartRender::getFireboxActive),
            BlockState.CODEC.fieldOf("casing_block").forGetter(BoilerMultiPartRender::getCasing)
    ).apply(instance, BoilerMultiPartRender::new));
    public static final DynamicRenderType<MultiblockControllerMachine, BoilerMultiPartRender> TYPE = new DynamicRenderType<>(BoilerMultiPartRender.CODEC);
    // spotless:on

    @Getter
    private final BlockState fireboxIdle, fireboxActive;
    @Getter
    private final BlockState casing;

    public BoilerMultiPartRender(BoilerFireboxType fireboxType, Supplier<? extends Block> casingBlock) {
        this(GTBlocks.ALL_FIREBOXES.get(fireboxType).getDefaultState(),
                GTBlocks.ALL_FIREBOXES.get(fireboxType).getDefaultState().setValue(GTBlockStateProperties.ACTIVE, true),
                casingBlock.get().defaultBlockState());
    }

    public BoilerMultiPartRender(BlockState fireboxIdle, BlockState fireboxActive, BlockState casing) {
        this.fireboxIdle = fireboxIdle;
        this.fireboxActive = fireboxActive;
        this.casing = casing;
    }

    @Override
    public DynamicRenderType<MultiblockControllerMachine, BoilerMultiPartRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(MultiblockControllerMachine machine, Vec3 cameraPos) {
        return false;
    }

    @Override
    public boolean isBlockEntityRenderer() {
        return false;
    }

    @Override
    public PartModelResult collectPartModel(ControllerPartRenderState controller, BlockStateModelSet modelSet,
                                            BlockAndTintGetter level, BlockPos partPos, BlockState partState,
                                            RandomSource random) {
        Direction relativeDown = RelativeDirection.DOWN.getRelativeFacing(controller.frontFacing(),
                controller.upwardsFacing(), controller.flipped());
        int belowController = controller.controllerPos().relative(relativeDown).get(relativeDown.getAxis());
        int partCoordinate = partPos.get(relativeDown.getAxis());
        BlockState selectedState = belowController == partCoordinate
                ? (controller.active() ? this.fireboxActive : this.fireboxIdle)
                : this.casing;

        BlockStateModel model = modelSet.get(selectedState);
        long seed = random.nextLong();
        List<BlockStateModelPart> parts = new java.util.ArrayList<>();
        model.collectParts(level, partPos, selectedState, RandomSource.create(seed), parts);
        Object childGeometryKey = model.createGeometryKey(level, partPos, selectedState, RandomSource.create(seed));
        return new PartModelResult(parts, new BoilerGeometryKey(selectedState, childGeometryKey));
    }

    private record BoilerGeometryKey(BlockState selectedState, Object childGeometryKey) {}

}
