package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.client.renderer.item.MachineItemRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.item.MachineItemRenderSnapshotProvider;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderSnapshot;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.storage.CreativeChestMachine;
import com.gregtechceu.gtceu.common.machine.storage.QuantumChestMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import static com.gregtechceu.gtceu.utils.GTMatrixUtils.*;

public class QuantumChestItemRender extends DynamicRender<QuantumChestMachine, QuantumChestItemRender>
                                    implements MachineItemRenderSnapshotProvider {

    // spotless:off
    public static final MapCodec<QuantumChestItemRender> CODEC = MapCodec.unit(QuantumChestItemRender::new);
    public static final DynamicRenderType<QuantumChestMachine, QuantumChestItemRender> TYPE = new DynamicRenderType<>(QuantumChestItemRender.CODEC);
    // spotless:on

    private static @Nullable Item CREATIVE_CHEST_ITEM = null;

    public QuantumChestItemRender() {}

    @Override
    public DynamicRenderType<QuantumChestMachine, QuantumChestItemRender> getType() {
        return TYPE;
    }

    @Override
    public @Nullable MachineItemRenderSnapshot extractItemRenderState(ItemStack stack) {
        if (!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        if (CREATIVE_CHEST_ITEM == null) CREATIVE_CHEST_ITEM = GTMachines.CREATIVE_ITEM.getItem();
        var data = com.gregtechceu.gtceu.api.item.data.ItemStackData.read(stack);
        ItemStack stored = com.gregtechceu.gtceu.utils.data.StackPersistence.loadItem(
                data.getCompoundOrEmpty("stored"));
        if (stored.isEmpty()) return null;

        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemModelResolver().updateForTopItem(itemState, stored.copy(), ItemDisplayContext.FIXED,
                minecraft.level, null, Item.getId(stored.getItem()) + stored.getDamageValue());
        float totalTick = getRenderTicks(minecraft);
        return new ChestItemSnapshot(itemState, totalTick, data.getLongOr("storedAmount", 0L),
                stack.is(CREATIVE_CHEST_ITEM));
    }

    @Override
    public DynamicRenderSnapshot extractRenderState(QuantumChestMachine machine, float partialTicks) {
        ItemStack stack = machine.getStored();
        if (stack.isEmpty()) stack = machine.getLockedItem();
        if (stack.isEmpty()) return null;

        ItemStack displayStack = stack.copy();
        ItemStackRenderState itemState = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemState, displayStack,
                ItemDisplayContext.FIXED, machine.getLevel(), null,
                Item.getId(displayStack.getItem()) + displayStack.getDamageValue());
        float totalTick = machine.getLevel().getGameTime() + partialTicks;
        return new QuantumChestSnapshot(itemState, totalTick, machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.getStoredAmount(), machine instanceof CreativeChestMachine);
    }

    private static float getRenderTicks(Minecraft minecraft) {
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return minecraft.level == null ? partialTick : minecraft.level.getGameTime() + partialTick;
    }

    @Override
    public void submitRenderState(DynamicRenderSnapshot state, PoseStack poseStack, SubmitNodeCollector collector,
                                  CameraRenderState camera) {
        if (!(state instanceof QuantumChestSnapshot snapshot)) return;

        poseStack.pushPose();
        setupModelRotation(snapshot.frontFacing(), snapshot.upwardsFacing(), poseStack);
        poseStack.translate(0.5f, 0.5f, 0.5f);
        if (snapshot.frontFacing().getAxis() == Direction.Axis.Y) {
            poseStack.mulPose(getRotation(Direction.NORTH, snapshot.frontFacing()));
        }
        poseStack.mulPose(new Quaternionf().rotateY(snapshot.totalTick() * Mth.TWO_PI / 80));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        snapshot.itemState().submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        submitAmountText(poseStack, collector, snapshot.frontFacing(), snapshot.storedAmount(), snapshot.creative());
    }

    private record QuantumChestSnapshot(ItemStackRenderState itemState, float totalTick, Direction frontFacing,
                                        Direction upwardsFacing, long storedAmount, boolean creative)
            implements DynamicRenderSnapshot {}

    private record ChestItemSnapshot(ItemStackRenderState itemState, float totalTick, long storedAmount,
                                     boolean creative) implements MachineItemRenderSnapshot {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY(this.totalTick() * Mth.TWO_PI / 80));
            poseStack.scale(0.6f, 0.6f, 0.6f);
            this.itemState().submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
            submitAmountText(poseStack, collector, Direction.NORTH, this.storedAmount(), this.creative());
            poseStack.popPose();
        }
    }

    public static void setupModelRotation(Direction frontFacing, Direction upwardFacing, PoseStack poseStack) {

        poseStack.translate(0.5f, 0.5f, 0.5f);
        float roll = frontFacing.getAxis().isHorizontal() ?
                frontAxisRollAngle(frontFacing, upwardFacing, Direction.UP) :
                upwardFacingAngle(upwardFacing) + (upwardFacing.getAxis() == Direction.Axis.X ? Mth.PI : 0);
        rotateMatrix(poseStack.last().pose(), roll, getDirectionAxis(frontFacing));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
    }

    public static void submitAmountText(PoseStack poseStack, SubmitNodeCollector collector, Direction frontFacing,
                                        long storedAmount, boolean isCreative) {
        poseStack.pushPose();
        poseStack.translate(frontFacing.getStepX() * -1 / 16f, frontFacing.getStepY() * -1 / 16f,
                frontFacing.getStepZ() * -1 / 16f);

        RenderUtil.moveToFace(poseStack, 0.5f, 0.5f, 0.5f, frontFacing);
        Direction spin = frontFacing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.NORTH;
        RenderUtil.rotateToFace(poseStack, frontFacing, spin);
        poseStack.scale(1f / 64, 1f / 64, 0);
        poseStack.translate(-32, -32, 0);

        String text = isCreative ? "∞" : storedAmount <= 0 ? "*" : FormattingUtil.formatNumberReadable(storedAmount, false);
        Font font = Minecraft.getInstance().font;
        float textX = 32.0f;
        float textY = 38.0f;
        if (isCreative) {
            poseStack.translate(textX, textY, 0);
            poseStack.scale(3.0f, 3.0f, 1.0f);
            poseStack.translate(-textX, -textY, 0);
        }
        collector.submitText(poseStack, textX - font.width(text) / 2.0f, textY - font.lineHeight / 2.0f,
                font.split(net.minecraft.network.chat.Component.literal(text), Integer.MAX_VALUE).getFirst(), false,
                Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, 0xffffffff, 0, 0);
        poseStack.popPose();
    }

}
