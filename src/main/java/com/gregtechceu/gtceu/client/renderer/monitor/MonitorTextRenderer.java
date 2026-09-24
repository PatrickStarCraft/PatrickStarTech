package com.gregtechceu.gtceu.client.renderer.monitor;

import com.gregtechceu.gtceu.api.placeholder.GraphicsComponent;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.utils.data.StackPersistence;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonitorTextRenderer implements IMonitorRenderer, IMonitorRenderSnapshotProvider {

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

            MonitorRenderSnapshot snapshot = extractPlaceholderSnapshot(graphicsComponent.rendererId(),
                    graphicsComponent.renderData(), machine, group, partialTick);
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

    private static MonitorRenderSnapshot extractPlaceholderSnapshot(String rendererId, CompoundTag data,
                                                                     CentralMonitorMachine machine,
                                                                     MonitorGroup group, float partialTick) {
        return switch (rendererId) {
            case "rect" -> new QuadSnapshot(
                    new float[] { 0, data.getFloatOr("height", 0.0F), data.getFloatOr("width", 0.0F),
                            data.getFloatOr("height", 0.0F), data.getFloatOr("width", 0.0F), 0, 0, 0 },
                    new int[] { data.getIntOr("color", 0), data.getIntOr("color", 0), data.getIntOr("color", 0),
                            data.getIntOr("color", 0) });
            case "quad" -> new QuadSnapshot(
                    new float[] { data.getFloat("x1").orElse(0.0F), data.getFloat("y1").orElse(0.0F),
                            data.getFloat("x2").orElse(0.0F), data.getFloat("y2").orElse(0.0F),
                            data.getFloat("x3").orElse(0.0F), data.getFloat("y3").orElse(0.0F),
                            data.getFloat("x4").orElse(0.0F), data.getFloat("y4").orElse(0.0F) },
                    new int[] { data.getInt("color1").orElse(0), data.getInt("color2").orElse(0),
                            data.getInt("color3").orElse(0), data.getInt("color4").orElse(0) });
            case "module" -> extractModuleSnapshot(data, machine, group, partialTick);
            default -> null;
        };
    }

    private static MonitorRenderSnapshot extractModuleSnapshot(CompoundTag data, CentralMonitorMachine machine,
                                                                MonitorGroup group, float partialTick) {
        ItemStack stack = StackPersistence.loadItem(data);
        if (!(stack.getItem() instanceof IComponentItem componentItem)) return null;
        List<MonitorRenderSnapshot> modules = new ArrayList<>();
        for (IItemComponent component : componentItem.getComponents()) {
            if (!(component instanceof IMonitorModuleItem module)) continue;
            IMonitorRenderer renderer = module.getRenderer(stack, machine, group);
            if (!(renderer instanceof IMonitorRenderSnapshotProvider provider)) continue;
            MonitorRenderSnapshot snapshot = provider.extractRenderState(machine, group, partialTick);
            if (snapshot != null) modules.add(snapshot);
        }
        return modules.isEmpty() ? null : new ModuleSnapshot(modules);
    }

    @Override
    public void render(CentralMonitorMachine machine, MonitorGroup group, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (group.isEmpty()) return;
        BlockPos rel = group.getRow(0, machine::toRelative).get(0);
        int row = 0;
        int columns = group.getRow(0, machine::toRelative).size();
        poseStack.translate(rel.getX(), rel.getY(), rel.getZ());
        int layer = 0;
        for (GraphicsComponent graphics : text.getGraphics()) {
            if (graphics.x() < 0 || graphics.y() < 0) continue;
            float maxX = graphics.x2();
            float maxY = graphics.y2();
            if (maxX == Math.floor(maxX)) maxX--;
            if (maxY == Math.floor(maxY)) maxY--;
            BlockPos relativePos = rel.offset(Mth.floor(maxX), Mth.floor(maxY), 0);
            if (!group.getMonitorPositions().stream().map(machine::toRelative).toList().contains(relativePos))
                continue;
            poseStack.pushPose();
            poseStack.translate(graphics.x(), graphics.y(), layer * .001f);
            graphics.get().render(machine, group, partialTick, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
            layer++;
        }
        poseStack.translate(0, 0, layer * .001f);
        poseStack.scale(TEXT_SCALE * scale, TEXT_SCALE * scale, TEXT_SCALE * scale);
        float y = 9;
        for (Component s : text) {
            boolean didAnything = false;
            for (FormattedCharSequence line : Minecraft.getInstance().font.split(s,
                    Math.round(columns * 135 / scale))) {
                if (y >= 144) {
                    try {
                        row++;
                        columns = group.getRow(row, machine::toRelative).size();
                        y -= 144;
                        poseStack.translate(-rel.getX() / (TEXT_SCALE * scale), -rel.getY() / (TEXT_SCALE * scale),
                                -rel.getZ() / (TEXT_SCALE * scale));
                        rel = group.getRow(row, machine::toRelative).get(0);
                        poseStack.translate(rel.getX() / (TEXT_SCALE * scale), rel.getY() / (TEXT_SCALE * scale),
                                rel.getZ() / (TEXT_SCALE * scale));
                    } catch (IndexOutOfBoundsException e) {
                        return;
                    }
                }
                Minecraft.getInstance().font.drawInBatch(
                        line,
                        9, y,
                        0xFFFFFF,
                        false,
                        poseStack.last().pose(),
                        buffer,
                        Font.DisplayMode.NORMAL,
                        0,
                        LightCoordsUtil.FULL_BRIGHT);
                y += Minecraft.getInstance().font.lineHeight * scale;
                didAnything = true;
            }
            if (!didAnything) {
                y += Minecraft.getInstance().font.lineHeight * scale;
            }
        }
    }

    private record GraphicEntry(float x, float y, float depth, MonitorRenderSnapshot snapshot) {}

    private record QuadSnapshot(float[] positions, int[] colors) implements MonitorRenderSnapshot {
        private QuadSnapshot {
            positions = positions.clone();
            colors = colors.clone();
        }

        @Override
        public float[] positions() {
            return this.positions.clone();
        }

        @Override
        public int[] colors() {
            return this.colors.clone();
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            float[] vertices = this.positions;
            int[] vertexColors = this.colors;
            collector.submitCustomGeometry(poseStack, GTRenderTypes.getMonitor(), (pose, buffer) -> {
                Matrix4f matrix = pose.pose();
                for (int i = 0; i < 4; i++) {
                    buffer.addVertex(matrix, vertices[i * 2], vertices[i * 2 + 1], 0)
                            .setColor(vertexColors[i]).setLight(LightCoordsUtil.FULL_BRIGHT);
                }
            });
        }
    }

    private record ModuleSnapshot(List<MonitorRenderSnapshot> modules) implements MonitorRenderSnapshot {
        private ModuleSnapshot {
            modules = List.copyOf(modules);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            for (MonitorRenderSnapshot module : this.modules) module.submit(poseStack, collector);
        }
    }

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
