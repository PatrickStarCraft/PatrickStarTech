package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.util.RenderUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CoverTextRenderer implements IDynamicCoverRenderer {

    private static final float TEXT_SCALE = 1 / 144f;

    @Setter
    private Supplier<List<? extends Component>> text;

    public CoverTextRenderer(Supplier<List<? extends Component>> text) {
        this.text = text;
    }

    @Override
    public DynamicCoverRenderSnapshot extractRenderState(MetaMachine machine, Direction face, float partialTick) {
        Font font = Minecraft.getInstance().font;
        List<TextLine> lines = new ArrayList<>();
        int y = 0;
        for (Component component : this.text.get()) {
            boolean didAnything = false;
            for (FormattedCharSequence line : font.split(component, 90)) {
                if (y >= 90) break;
                lines.add(new TextLine(line, y));
                y += font.lineHeight;
                didAnything = true;
            }
            if (y >= 90) break;
            if (!didAnything) y += font.lineHeight;
        }
        return new CoverTextSnapshot(face, List.copyOf(lines));
    }

    private record TextLine(FormattedCharSequence text, int y) {}

    private record CoverTextSnapshot(Direction face, List<TextLine> lines) implements DynamicCoverRenderSnapshot {
        private CoverTextSnapshot {
            lines = List.copyOf(lines);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
            if (this.lines.isEmpty()) return;
            poseStack.pushPose();
            RenderUtil.moveToFace(poseStack, 0.5f, 0.5f, 0.5f, this.face);
            RenderUtil.rotateToFace(poseStack, this.face, Direction.NORTH);
            poseStack.translate(-0.5f + 3 / 16f, -0.5f + 3 / 16f, 0.01f);
            poseStack.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
            for (TextLine line : this.lines) {
                collector.submitText(poseStack, 0, line.y(), line.text(), false, Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT, 0xff72e500, 0, 0);
            }
            poseStack.popPose();
        }
    }
}
