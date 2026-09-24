package com.gregtechceu.gtceu.client.renderer.monitor;

import com.gregtechceu.gtceu.api.placeholder.GraphicsComponent;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonitorTextRenderer implements IMonitorRenderer {

    private static final float TEXT_SCALE = 1 / 144f;
    private final MultiLineComponent text;
    private final float scale;

    public MonitorTextRenderer(MultiLineComponent text, double scale) {
        this.text = text;
        this.scale = (float) scale;
    }

    @Override
    public MonitorRenderSnapshot extractRenderState(CentralMonitorMachine machine, MonitorGroup group,
                                                    float partialTick) {
        if (group.isEmpty()) return null;
        Font font = Minecraft.getInstance().font;
        BlockPos rowStart = group.getRow(0, machine::toRelative).get(0);
        int row = 0;
        int columns = group.getRow(0, machine::toRelative).size();

        Set<BlockPos> monitorPositions = new HashSet<>();
        group.getMonitorPositions().stream().map(machine::toRelative).forEach(monitorPositions::add);
        List<GraphicEntry> graphics = new ArrayList<>();
        int layer = 0;
        for (GraphicsComponent graphicsComponent : this.text.getGraphics()) {
            if (graphicsComponent.x() < 0 || graphicsComponent.y() < 0) continue;
            float maxX = graphicsComponent.x2();
            float maxY = graphicsComponent.y2();
            if (maxX == Math.floor(maxX)) maxX--;
            if (maxY == Math.floor(maxY)) maxY--;
            BlockPos relativePos = rowStart.offset(Mth.floor(maxX), Mth.floor(maxY), 0);
            if (!monitorPositions.contains(relativePos)) continue;

            MonitorRenderSnapshot snapshot = graphicsComponent.extractRenderState(machine, group, partialTick);
            if (snapshot != null) {
                graphics.add(new GraphicEntry(graphicsComponent.x(), graphicsComponent.y(), layer * 0.001f,
                        snapshot));
            }
            layer++;
        }

        List<TextLine> lines = new ArrayList<>();
        float y = 9;
        for (Component component : this.text) {
            boolean didAnything = false;
            for (FormattedCharSequence line : font.split(component, Math.round(columns * 135 / this.scale))) {
                if (y >= 144) {
                    try {
                        row++;
                        columns = group.getRow(row, machine::toRelative).size();
                        y -= 144;
                        rowStart = group.getRow(row, machine::toRelative).get(0);
                    } catch (IndexOutOfBoundsException exception) {
                        return new MonitorTextSnapshot(this.scale, graphics, lines);
                    }
                }
                lines.add(new TextLine(new BlockPos(rowStart.getX(), rowStart.getY(), rowStart.getZ()), y, line));
                y += font.lineHeight * this.scale;
                didAnything = true;
            }
            if (!didAnything) y += font.lineHeight * this.scale;
        }
        return new MonitorTextSnapshot(this.scale, graphics, lines);
    }

    private record GraphicEntry(float x, float y, float depth, MonitorRenderSnapshot snapshot) {}

    private record TextLine(BlockPos origin, float y, FormattedCharSequence text) {}

    private record MonitorTextSnapshot(float scale, List<GraphicEntry> graphics, List<TextLine> lines)
            implements MonitorRenderSnapshot {
        private MonitorTextSnapshot {
            graphics = List.copyOf(graphics);
            lines = List.copyOf(lines);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            for (GraphicEntry graphic : this.graphics) {
                poseStack.pushPose();
                poseStack.translate(graphic.x(), graphic.y(), graphic.depth());
                graphic.snapshot().submit(poseStack, collector);
                poseStack.popPose();
            }

            for (TextLine line : this.lines) {
                poseStack.pushPose();
                poseStack.translate(line.origin().getX(), line.origin().getY(), line.origin().getZ());
                poseStack.scale(TEXT_SCALE * this.scale, TEXT_SCALE * this.scale, TEXT_SCALE * this.scale);
                collector.submitText(poseStack, 9, line.y(), line.text(), false, Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT, 0xffffffff, 0, 0);
                poseStack.popPose();
            }
        }
    }
}
