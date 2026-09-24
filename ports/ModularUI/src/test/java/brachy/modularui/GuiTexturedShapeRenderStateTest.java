package brachy.modularui;

import brachy.modularui.drawable.GuiTexturedShapeRenderState;
import brachy.modularui.drawable.GuiTexturedShapeRenderState.Vertex;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiTexturedShapeRenderStateTest {
    private static final List<Vertex> STRIP = List.of(
            new Vertex(0, 0, 0, 0, 1),
            new Vertex(0, 2, 1, 0, 2),
            new Vertex(4, 2, 0, 1, 3),
            new Vertex(4, 0, 1, 1, 4));

    @Test
    void transformsUvVerticesAndPreservesStripWinding() {
        var state = GuiTexturedShapeRenderState.extract(new Matrix3x2f().translate(2.25f, -1.5f),
                TextureSetup.noTexture(), null, STRIP);

        assertNotNull(state);
        assertEquals(new ScreenRectangle(2, -2, 5, 3), state.bounds());
        assertEquals(List.of(
                new Vertex(2.25f, -1.5f, 0, 0, 1),
                new Vertex(2.25f, 0.5f, 1, 0, 2),
                new Vertex(6.25f, 0.5f, 0, 1, 3),
                new Vertex(6.25f, 0.5f, 0, 1, 3),
                new Vertex(6.25f, 0.5f, 0, 1, 3),
                new Vertex(2.25f, 0.5f, 1, 0, 2),
                new Vertex(6.25f, -1.5f, 1, 1, 4),
                new Vertex(6.25f, -1.5f, 1, 1, 4)), state.vertices());
    }

    @Test
    void clipsAndSkipsInvisibleTexturedGeometry() {
        var clip = new ScreenRectangle(3, 1, 2, 1);
        var state = GuiTexturedShapeRenderState.extract(new Matrix3x2f(), TextureSetup.noTexture(), clip, STRIP);

        assertNotNull(state);
        assertSame(clip, state.scissorArea());
        assertEquals(new ScreenRectangle(3, 1, 1, 1), state.bounds());
        assertNull(GuiTexturedShapeRenderState.extract(new Matrix3x2f(), TextureSetup.noTexture(),
                new ScreenRectangle(9, 9, 1, 1), STRIP));
        assertNull(GuiTexturedShapeRenderState.extract(new Matrix3x2f(), TextureSetup.noTexture(), null,
                STRIP.subList(0, 2)));
    }

    /** Allows isolated checks while unrelated legacy sources still prevent the root test task. */
    public static void main(String[] args) {
        var test = new GuiTexturedShapeRenderStateTest();
        test.transformsUvVerticesAndPreservesStripWinding();
        test.clipsAndSkipsInvisibleTexturedGeometry();
        System.out.println("GUI textured geometry: 2 checks passed");
    }
}
