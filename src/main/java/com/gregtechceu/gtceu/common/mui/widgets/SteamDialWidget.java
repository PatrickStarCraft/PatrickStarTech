package com.gregtechceu.gtceu.common.mui.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.GuiShapeBuilder;
import brachy.modularui.drawable.GuiShapeRenderState;
import brachy.modularui.drawable.GuiTexturedShapeRenderState;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.viewport.GuiContext;
import brachy.modularui.theme.WidgetTheme;
import brachy.modularui.utils.MUIRenderTypes;
import brachy.modularui.utils.Color;
import brachy.modularui.value.sync.DoubleSyncValue;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
public class SteamDialWidget implements IDrawable {

    private DoubleSyncValue progress;
    @Setter
    private float minAngle;
    @Setter
    private float maxAngle;
    @Setter
    private int color;
    private float lastAngle = Float.NaN;
    @Setter
    private UITexture texture;

    public SteamDialWidget(DoubleSyncValue progress) {
        this.progress = progress;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GuiGraphicsExtractor graphics = context.getGraphics();
        final float progressPercent = Mth.clamp(progress.getFloatValue(), 0.0f, 1.0f);
        final float angle = Mth.lerp(progressPercent, this.minAngle, this.maxAngle);
        lastAngle = Float.isNaN(lastAngle) ? angle : (lastAngle + angle) / 2.0f;

        final float sinAngle = Mth.sin(-lastAngle);
        final float cosAngle = Mth.cos(-lastAngle);
        height /= 2.0f;
        int alpha = Color.getAlpha(color), red = Color.getRed(color), green = Color.getGreen(color), blue = Color.getBlue(color);
        int packedColor = (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);

        float x0 = x + width * cosAngle, y0 = y + width * sinAngle;
        float x1 = x + height * sinAngle, y1 = y - height * cosAngle;
        float x2 = x - height * sinAngle, y2 = y + height * cosAngle;
        float x3 = x - height * cosAngle, y3 = y - height * sinAngle;
        if (texture == null) {
            GuiShapeBuilder vertices = MUIRenderTypes.guiTriangleStrip();
            vertices.addVertex(x0, y0, 0).setColor(red, green, blue, alpha);
            vertices.addVertex(x1, y1, 0).setColor(red, green, blue, alpha);
            vertices.addVertex(x2, y2, 0).setColor(red, green, blue, alpha);
            vertices.addVertex(x3, y3, 0).setColor(red, green, blue, alpha);
            vertices.submit(graphics);
        } else {
            GuiTexturedShapeRenderState.submitStrip(graphics, texture.location, List.of(
                    new GuiTexturedShapeRenderState.Vertex(x0, y0, 0, 0, packedColor),
                    new GuiTexturedShapeRenderState.Vertex(x1, y1, 1, 0, packedColor),
                    new GuiTexturedShapeRenderState.Vertex(x2, y2, 0, 1, packedColor),
                    new GuiTexturedShapeRenderState.Vertex(x3, y3, 1, 1, packedColor)));
        }
    }
}
