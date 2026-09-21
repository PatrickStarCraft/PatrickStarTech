package com.gregtechceu.gtceu.common.item.modules;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.data.ItemStackData;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.placeholder.GraphicsComponent;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderHandler;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorTextRenderer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import brachy.modularui.api.IPanelHandler;
import brachy.modularui.value.sync.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class TextModuleBehaviour implements IMonitorModuleItem, IAddInformation {

    private PlaceholderContext getContext(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        UUID id = ItemStackData.read(stack).read("placeholderUUID", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        if (id == null) {
            id = UUID.randomUUID();
            UUID newId = id;
            ItemStackData.update(stack, tag -> tag.store("placeholderUUID", net.minecraft.core.UUIDUtil.CODEC, newId));
        }
        return new PlaceholderContext(
                group.getTargetLevel(machine.getLevel()),
                group.getTarget(machine.getLevel()),
                group.getTargetCoverSide(),
                group.getPlaceholderSlotsHandler(),
                group.getTargetCover(machine.getLevel()),
                group,
                null,
                id);
    }

    private void updateText(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        MultiLineComponent text = PlaceholderHandler.processPlaceholders(
                getPlaceholderText(stack), getContext(stack, machine, group));
        ItemStackData.update(stack, tag -> tag.put("text",
                text.withStyle(style -> style.withFont(GTGuiTextures.MONOCRAFT_FONT)).toTag()));
    }

    @Override
    public void tick(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        if (!isPaused(stack))
            this.updateText(stack, machine, group);
    }

    @Override
    public IMonitorRenderer getRenderer(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        return new MonitorTextRenderer(
                getText(stack),
                Math.max(getScale(stack), .0001));
    }

    @Override
    public IPanelHandler createModularPanel(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group,
                                            PanelSyncManager syncManager) {
        PlaceholderContext ctx = getContext(stack, machine, group);
        StringSyncValue code = SyncHandlers.string(
                () -> getPlaceholderText(stack),
                s -> setPlaceholderText(stack, s))
                .allowC2S();
        DoubleSyncValue scale = SyncHandlers.doubleNumber(
                () -> getScale(stack),
                s -> setScale(stack, s))
                .allowC2S();
        BooleanSyncValue pause = SyncHandlers.bool(() -> isPaused(stack), p -> setPaused(stack, p))
                .allowC2S();
        Runnable updateText = () -> updateText(stack, machine, group);
        assert ctx.itemStackHandler() != null;
        return PlaceholderHandler.createPlaceholderEditor("text_module_" + group.getName(), syncManager, ctx, code,
                scale, null, pause,
                updateText);
    }

    @Override
    public String getType() {
        return "text";
    }

    public MultiLineComponent getText(ItemStack stack) {
        return MultiLineComponent.fromTag(ItemStackData.read(stack).get("text"));
    }

    public double getScale(ItemStack stack) {
        return Math.max(ItemStackData.read(stack).getDoubleOr("scale", 1), .0001);
    }

    public void setScale(ItemStack stack, double scale) {
        ItemStackData.update(stack, tag -> tag.putDouble("scale", scale));
    }

    public void setPaused(ItemStack stack, boolean paused) {
        ItemStackData.update(stack, tag -> tag.putBoolean("paused", paused));
    }

    public boolean isPaused(ItemStack stack) {
        return ItemStackData.read(stack).getBooleanOr("paused", false);
    }

    public void setPlaceholderText(ItemStack stack, String text) {
        ListTag listTag = new ListTag();
        for (String line : text.split("\n")) listTag.add(StringTag.valueOf(line.replaceAll("\r", "")));
        ItemStackData.update(stack, tag -> tag.put("formatStringLines", listTag));
    }

    public String getPlaceholderText(ItemStack stack) {
        StringBuilder formatStringLines = new StringBuilder();
        ListTag tag = com.gregtechceu.gtceu.utils.data.TypedTagList.read(ItemStackData.read(stack), "formatStringLines", StringTag.TAG_STRING);
        for (Tag value : tag) {
            formatStringLines.append(value.asString().orElse("")).append('\n');
        }
        return formatStringLines.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.literal("Placeholder text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(MultiLineComponent.literal(getPlaceholderText(stack)));
            tooltipComponents.add(Component.literal("Processed text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(getText(stack));
            tooltipComponents.add(Component.literal("Graphics components:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(getText(stack).getGraphics().stream()
                    .map(GraphicsComponent::rendererId)
                    .map(Component::literal)
                    .toList());
        }
    }
}
