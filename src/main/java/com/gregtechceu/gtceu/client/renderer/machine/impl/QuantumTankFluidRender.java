package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.client.renderer.item.MachineItemRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.item.MachineItemRenderSnapshotProvider;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.storage.CreativeTankMachine;
import com.gregtechceu.gtceu.common.machine.storage.QuantumTankMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;

import java.util.EnumSet;

import javax.annotation.Nullable;

import static com.gregtechceu.gtceu.client.renderer.machine.impl.QuantumChestItemRender.*;
import static com.gregtechceu.gtceu.common.machine.storage.QuantumTankMachine.TANK_CAPACITY;

public class QuantumTankFluidRender extends DynamicRender<QuantumTankMachine, QuantumTankFluidRender>
                                   implements MachineItemRenderSnapshotProvider {

    // spotless:off
    public static final MapCodec<QuantumTankFluidRender> CODEC = MapCodec.unit(QuantumTankFluidRender::new);
    public static final DynamicRenderType<QuantumTankMachine, QuantumTankFluidRender> TYPE = new DynamicRenderType<>(QuantumTankFluidRender.CODEC);
    // spotless:on

    private static final float MIN = 0.16f;
    private static final float MAX = 0.84f;

    private static @Nullable Item CREATIVE_FLUID_ITEM = null;

    public QuantumTankFluidRender() {}

    @Override
    public DynamicRenderType<QuantumTankMachine, QuantumTankFluidRender> getType() {
        return TYPE;
    }

    @Override
    public @Nullable MachineItemRenderSnapshot extractItemRenderState(ItemStack stack) {
        if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        if (CREATIVE_FLUID_ITEM == null) CREATIVE_FLUID_ITEM = GTMachines.CREATIVE_FLUID.getItem();

        var data = com.gregtechceu.gtceu.api.item.data.ItemStackData.read(stack);
        FluidStack stored = com.gregtechceu.gtceu.utils.data.StackPersistence.loadFluid(
                data.getCompoundOrEmpty("stored"));
        if (stored.isEmpty()) return null;
        Fluid fluid = stored.getFluid();
        long storedAmount = data.getLongOr("storedAmount", 0L);
        if (storedAmount == 0) storedAmount = stored.getAmount();
        long maxAmount = stack.getItem() instanceof MetaMachineItem machineItem ?
                TANK_CAPACITY.getLong(machineItem.getDefinition()) : 0;
        boolean creative = stack.is(CREATIVE_FLUID_ITEM);
        boolean gas = fluid.getFluidType().isLighterThanAir();
        int tint = GTUtil.getFluidColor(stored);
        TankFluidSnapshot tank = new TankFluidSnapshot(fluid, storedAmount, maxAmount, Direction.NORTH, Direction.UP,
                creative, gas, tint);
        return new TankItemSnapshot(tank);
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(QuantumTankMachine machine, float partialTicks) {
        FluidStack stored = machine.getStored();
        FluidStack fluidStack = stored.isEmpty() ? machine.getLockedFluid() : stored;
        if (fluidStack.isEmpty()) return null;

        Fluid fluid = fluidStack.getFluid();
        FluidModel fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(fluid.defaultFluidState());
        int tint = fluidModel.fluidTintSource() == null ? -1 : fluidModel.fluidTintSource().colorAsStack(fluidStack);
        tint |= 0xff000000;
        return new TankFluidSnapshot(fluid, machine.getStoredAmount(), machine.getMaxAmount(),
                machine.getFrontFacing(), machine.getUpwardsFacing(), machine instanceof CreativeTankMachine,
                fluid.getFluidType().isLighterThanAir(), tint);
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                  CameraRenderState camera) {
        if (!(state instanceof TankFluidSnapshot snapshot)) return;

        poseStack.pushPose();
        setupModelRotation(snapshot.frontFacing(), snapshot.upwardsFacing(), poseStack);
        RenderType renderType = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                drawTankGeometry(snapshot, pose, buffer));
        submitAmountText(poseStack, collector, snapshot.frontFacing(), snapshot.storedAmount(), snapshot.creative());
        poseStack.popPose();
    }

    private static void drawTankGeometry(TankFluidSnapshot snapshot, PoseStack.Pose pose, VertexConsumer builder) {
        var fluidSprite = RenderUtil.FluidTextureType.STILL.map(snapshot.fluid());
        Direction frontFacing = snapshot.frontFacing();
        EnumSet<Direction> sidesToRender = EnumSet.of(frontFacing);

        float percentFull = snapshot.creative() || snapshot.maxAmount() <= snapshot.storedAmount() ? 1f :
                (float) snapshot.storedAmount() / snapshot.maxAmount();
        float maxTop = snapshot.gas() ? MAX : MIN + percentFull * (MAX - MIN);
        float minBot = snapshot.gas() ? MIN + (1 - percentFull) * (MAX - MIN) : MIN;
        float minY, maxY, minZ, maxZ;
        if (frontFacing.getAxis() == Direction.Axis.Y) {
            minY = MIN;
            maxY = MAX;
            if (frontFacing == Direction.UP) {
                minZ = minBot;
                maxZ = maxTop;
                sidesToRender.add(snapshot.gas() ? Direction.SOUTH : Direction.NORTH);
            } else {
                minZ = 1 - maxTop;
                maxZ = 1 - minBot;
                sidesToRender.add(snapshot.gas() ? Direction.NORTH : Direction.SOUTH);
            }
        } else {
            minY = minBot;
            maxY = maxTop;
            minZ = MIN;
            maxZ = MAX;
            sidesToRender.add(snapshot.gas() ? Direction.DOWN : Direction.UP);
        }

        RenderBufferHelper.renderTexturedCube(builder, pose, sidesToRender, snapshot.tint(),
                LightCoordsUtil.FULL_BRIGHT, fluidSprite, MIN, minY, minZ, MAX, maxY, maxZ);
    }

    private record TankFluidSnapshot(Fluid fluid, long storedAmount, long maxAmount, Direction frontFacing,
                                     Direction upwardsFacing, boolean creative, boolean gas, int tint)
            implements DynamicRenderSnapshot {}

    private record TankItemSnapshot(TankFluidSnapshot tank) implements MachineItemRenderSnapshot {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            poseStack.pushPose();
            RenderType renderType = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    drawTankGeometry(this.tank(), pose, buffer));
            submitAmountText(poseStack, collector, Direction.NORTH, this.tank().storedAmount(),
                    this.tank().creative());
            poseStack.popPose();
        }
    }

}
