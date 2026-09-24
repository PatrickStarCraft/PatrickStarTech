package brachy.modularui.drawable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/** Immutable textured strip geometry for Minecraft's deferred GUI renderer. */
public record GuiTexturedShapeRenderState(List<Vertex> vertices, TextureSetup textureSetup,
                                          @Nullable ScreenRectangle scissorArea,
                                          @Nullable ScreenRectangle bounds) implements GuiElementRenderState {

    public GuiTexturedShapeRenderState {
        vertices = List.copyOf(vertices);
    }

    public record Vertex(float x, float y, float u, float v, int color) {}

    /** Records a textured triangle strip while preserving each vertex's position and UV. */
    public static void submitStrip(GuiGraphicsExtractor graphics, Identifier texture, List<Vertex> vertices) {
        if (vertices.size() < 3) return;

        int tint = GuiTint.get();
        if (tint != -1) {
            vertices = vertices.stream().map(vertex -> new Vertex(vertex.x(), vertex.y(), vertex.u(), vertex.v(),
                    ARGB.multiply(vertex.color(), tint))).toList();
        }

        var loadedTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        var textureSetup = TextureSetup.singleTexture(loadedTexture.getTextureView(), loadedTexture.getSampler());
        var state = extract(graphics.pose(), textureSetup, graphics.peekScissorStack(), vertices);
        if (state != null) graphics.submitGuiElementRenderState(state);
    }

    /** Transforms and triangulates a strip into the degenerate quads expected by the GUI renderer. */
    public static @Nullable GuiTexturedShapeRenderState extract(Matrix3x2fc pose, TextureSetup textureSetup,
                                                                 @Nullable ScreenRectangle scissor,
                                                                 List<Vertex> vertices) {
        if (vertices.size() < 3) return null;

        List<Vertex> transformed = new ArrayList<>(vertices.size());
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (Vertex vertex : vertices) {
            Vector2f point = pose.transformPosition(vertex.x(), vertex.y(), new Vector2f());
            transformed.add(new Vertex(point.x, point.y, vertex.u(), vertex.v(), vertex.color()));
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }

        List<Vertex> quads = new ArrayList<>((transformed.size() - 2) * 4);
        for (int i = 1; i < transformed.size() - 1; i++) {
            int first = i % 2 == 0 ? i : i - 1;
            int second = i % 2 == 0 ? i - 1 : i;
            Vertex last = transformed.get(i + 1);
            quads.add(transformed.get(first));
            quads.add(transformed.get(second));
            quads.add(last);
            quads.add(last);
        }

        int left = (int) Math.floor(minX), top = (int) Math.floor(minY);
        ScreenRectangle bounds = new ScreenRectangle(left, top,
                (int) Math.ceil(maxX) - left, (int) Math.ceil(maxY) - top);
        if (scissor != null) bounds = bounds.intersection(scissor);
        return bounds == null ? null : new GuiTexturedShapeRenderState(quads, textureSetup, scissor, bounds);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (Vertex vertex : vertices) {
            consumer.addVertex(vertex.x(), vertex.y(), 0).setUv(vertex.u(), vertex.v()).setColor(vertex.color());
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }
}
