package com.gregtechceu.gtceu.client.util;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.utils.GTMatrixUtils;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import brachy.modularui.drawable.GuiDraw;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.*;

import static net.minecraft.util.ARGB.*;

@OnlyIn(Dist.CLIENT)
public class RenderUtil {

    public enum FluidTextureType {

        STILL,
        FLOWING,
        OVERLAY;

        public TextureAtlasSprite map(Fluid fluid) {
            FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                    .get(fluid.defaultFluidState());
            return switch (this) {
                case STILL -> model.stillMaterial().sprite();
                case FLOWING -> model.flowingMaterial().sprite();
                case OVERLAY -> model.overlayMaterial() != null ? model.overlayMaterial().sprite() :
                        model.stillMaterial().sprite();
            };
        }
    }

    public static Vec3 vec3(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static Vector3f vec3f(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    // spotless:off
    private static final Map<Direction, Vector3fc[]> DIRECTION_POSITION_MAP = Util.make(new EnumMap<>(Direction.class), map -> {
        map.put(Direction.UP, new Vector3fc[] { vec3f(0, 1, 1), vec3f(1, 1, 1), vec3f(1, 1, 0), vec3f(0, 1, 0) });
        map.put(Direction.DOWN, new Vector3fc[] { vec3f(1, 0, 1), vec3f(0, 0, 1), vec3f(0, 0, 0), vec3f(1, 0, 0) });
        map.put(Direction.SOUTH, new Vector3fc[] { vec3f(1, 1, 0), vec3f(1, 0, 0), vec3f(0, 0, 0), vec3f(0, 1, 0) });
        map.put(Direction.NORTH, new Vector3fc[] { vec3f(0, 1, 1), vec3f(0, 0, 1), vec3f(1, 0, 1), vec3f(1, 1, 1) });
        map.put(Direction.EAST, new Vector3fc[] { vec3f(0, 1, 0), vec3f(0, 0, 0), vec3f(0, 0, 1), vec3f(0, 1, 1) });
        map.put(Direction.WEST, new Vector3fc[] { vec3f(1, 1, 1), vec3f(1, 0, 1), vec3f(1, 0, 0), vec3f(1, 1, 0) });
    });
    // spotless:on

    public static Vector3fc[] getVertices(Direction direction) {
        return DIRECTION_POSITION_MAP.get(direction);
    }

    // spotless:off
    private static final Map<Direction, Vector3fc> DIRECTION_NORMAL_MAP = Util.make(new EnumMap<>(Direction.class), map -> {
        map.put(Direction.UP, vec3f(0, 1, 0));
        map.put(Direction.DOWN, vec3f(0, 1, 0));
        map.put(Direction.SOUTH, vec3f(0, 0, 1));
        map.put(Direction.NORTH, vec3f(0, 0, 1));
        map.put(Direction.EAST, vec3f(1, 0, 0));
        map.put(Direction.WEST, vec3f(1, 0, 0));
    });
    // spotless:on

    public static Vector3fc getNormal(Direction direction) {
        return DIRECTION_NORMAL_MAP.get(direction);
    }

    public static int getFluidLight(Fluid fluid, BlockPos pos, @Nullable BlockAndLightGetter level) {
        if (level == null) level = Minecraft.getInstance().level;
        if (level == null) return 0;

        return LightCoordsUtil.getLightCoords(LightCoordsUtil.BrightnessGetter.DEFAULT, level,
                fluid.defaultFluidState().createLegacyBlock(), pos);
    }

    public static void vertex(PoseStack.Pose pose, VertexConsumer vertexConsumer,
                              Vector3fc pos, Vector3fc normal, float u, float v,
                              int argb, int packedOverlay, int packedLight) {
        /*
         * For future reference:
         * The order of the vertex calls is important.
         * Change it, and it'll break and complain that you didn't fill all elements (even though you did).
         */
        vertexConsumer.gtceu$vertex(pose.pose(), pos)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .gtceu$normal(pose.normal(), normal)
                ;
    }

    public static void vertex(PoseStack.Pose pose, VertexConsumer vertexConsumer,
                              float x, float y, float z,
                              int r, int g, int b, int a, float u, float v,
                              int packedOverlay, int packedLight,
                              float v0, float v1, float v2) {
        /*
         * For future reference:
         * The order of the vertex calls is important.
         * Change it, and it'll break and complain that you didn't fill all elements (even though you did).
         */
        vertexConsumer.addVertex(pose.pose(), x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, v0, v1, v2)
                ;
    }

    public static Vector3f transformVertex(Vector3fc vertex, Direction direction,
                                           float offsetX, float offsetY, float offsetZ) {
        float addX = offsetX, addY = offsetY, addZ = offsetZ;
        switch (direction) {
            case DOWN -> addY = -addY;
            case SOUTH -> addZ = -addZ;
            case EAST -> addX = -addX;
        }

        return new Vector3f(vertex).add(addX, addY, addZ);
    }

    public static @Nullable Fluid getRecipeFluidToRender(GTRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        var contents = new ObjectArrayList<Content>();
        var empty = new ArrayList<Content>();
        contents.addAll(recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, empty));
        contents.addAll(recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, empty));
        if (contents.isEmpty()) {
            return null;
        }

        var fluidContent = contents.stream()
                .filter(content -> content.content() instanceof FluidIngredient ingredient && !ingredient.isEmpty())
                .findAny();
        if (fluidContent.isEmpty()) {
            return null;
        }
        var ingredient = (FluidIngredient) fluidContent.get().content();

        var stacks = ingredient.getStacks();
        if (stacks.length == 0) {
            return null;
        }

        Fluid fluid = null;
        for (int i = 0; i < stacks.length && fluid == null; i++) {
            if (!stacks[i].isEmpty()) {
                fluid = stacks[i].getFluid();
            }
        }

        return fluid;
    }

    /**
     * Fills a GUI rectangle with a left-to-right color gradient. GUI render states use strata for ordering instead of
     * vertex z values; rotating the target's top-to-bottom gradient keeps the original horizontal color direction.
     */
    public static void fillHorizontalGradient(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2,
                                              int colorFrom, int colorTo) {
        graphics.nextStratum();
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(x1, y2);
            pose.rotate(-Mth.HALF_PI);
            graphics.fillGradient(0, 0, y2 - y1, x2 - x1, colorFrom, colorTo);
        } finally {
            pose.popMatrix();
        }
    }

    /**
     * Converts an (A)RGB integer color into an array of floats, for use in GL calls
     *
     * @return float[]{R, G, B, A}
     */
    public static float[] floats(int argb) {
        return new float[] {
                (float) (argb >> 16 & 255) / 255.0F,
                (float) (argb >> 8 & 255) / 255.0F,
                (float) (argb & 255) / 255.0F,
                (float) (argb >> 24 & 255) / 255.0F
        };
    }

    public static int interpolateColor(int color1, int color2, float blend) {
        int a1 = color1 >> 24 & 255;
        int r1 = color1 >> 16 & 255;
        int g1 = color1 >> 8 & 255;
        int b1 = color1 & 255;

        int a2 = color2 >> 24 & 255;
        int r2 = color2 >> 16 & 255;
        int g2 = color2 >> 8 & 255;
        int b2 = color2 & 255;

        int a = (int) (a1 * (1 - blend) + a2 * blend);
        int r = (int) (r1 * (1 - blend) + r2 * blend);
        int g = (int) (g1 * (1 - blend) + g2 * blend);
        int b = (int) (b1 * (1 - blend) + b2 * blend);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void moveToFace(PoseStack poseStack, Vector3fc pos, Direction face) {
        moveToFace(poseStack, pos.x(), pos.y(), pos.z(), face);
    }

    public static void moveToFace(PoseStack poseStack, float x, float y, float z, Direction face) {
        poseStack.translate(Math.fma(face.getStepX(), 0.5f, x),
                Math.fma(face.getStepY(), 0.5f, y),
                Math.fma(face.getStepZ(), 0.5f, z));
    }

    /**
     * Rotate the current coordinate system, so it is on the face of the given block side.
     * This can be used to render on the given face as if it was a 2D canvas,
     * where x+ is facing right and y+ is facing up.
     */
    public static void rotateToFace(PoseStack poseStack, Direction face, Direction spin) {
        float rotationAngle = Mth.HALF_PI * switch (face) {
            case UP, WEST -> 1;
            case DOWN, EAST -> -1;
            case SOUTH -> 2;
            case NORTH -> 0;
        };
        Quaternionf rotation = new Quaternionf();
        if (face.getAxis() == Direction.Axis.Y) {
            poseStack.scale(1.0f, -1.0f, 1.0f);
            rotation.rotateX(rotationAngle);
        } else {
            poseStack.scale(-1.0f, -1.0f, -1.0f);
            rotation.rotateY(rotationAngle);
        }
        rotation.rotateZ(getSpinAngle(spin, face));

        poseStack.mulPose(rotation);
    }

    private static float getSpinAngle(Direction spin, Direction face) {
        if (spin.getAxis() == Direction.Axis.Z && face == Direction.DOWN) {
            spin = spin.getOpposite();
        }
        return GTMatrixUtils.upwardFacingAngle(spin);
    }

    public static boolean renderResearchItemContent(GuiGraphicsExtractor graphics, Operation<Void> originalMethod,
                                                    @Nullable LivingEntity entity, @Nullable Level level,
                                                    ItemStack stack, int x, int y, int seed) {
        if (!Minecraft.getInstance().hasShiftDown()) return false;

        ResearchManager.ResearchItem researchData = ResearchManager.readResearchId(stack);
        if (researchData == null) return false;

        Collection<GTRecipe> recipes = researchData.recipeType().getDataStickEntry(researchData.researchId());
        if (recipes == null || recipes.isEmpty()) return false;

        for (var recipe : recipes) {
            // check item outputs first
            List<Content> outputs = recipe.getOutputContents(ItemRecipeCapability.CAP);
            if (!outputs.isEmpty()) {
                ItemStack[] items = com.gregtechceu.gtceu.api.recipe.ingredient.IngredientStacks.getItems(ItemRecipeCapability.CAP.of(outputs.get(0).content()));
                if (items.length > 0) {
                    ItemStack output = items[0];
                    if (!output.isEmpty() && !GTUtil.isSameItemSameTags(output, stack)) {
                        originalMethod.call(entity, level, output, x, y, seed);
                        return true;
                    }
                }
            }
            // if there are no item outputs, try to find a fluid output
            outputs = recipe.getOutputContents(FluidRecipeCapability.CAP);
            if (!outputs.isEmpty()) {
                FluidStack[] fluids = FluidRecipeCapability.CAP.of(outputs.get(0).content()).getStacks();
                if (fluids.length != 0) {
                    FluidStack output = fluids[0];
                    if (!output.isEmpty()) {
                        graphics.nextStratum();
                        GuiDraw.drawFluidTexture(graphics, output, x, y, 0, 0, 0);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
