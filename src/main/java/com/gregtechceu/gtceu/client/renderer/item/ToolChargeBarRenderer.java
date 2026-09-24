package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.client.util.RenderUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntIntPair;

public final class ToolChargeBarRenderer {

    private static final int BAR_W = 12;

    private static final int colorShadow = ARGB.color(255, 0, 0, 0);
    private static final int colorBarLeftEnergy = ARGB.color(255, 0, 101, 178);
    private static final int colorBarRightEnergy = ARGB.color(255, 217, 238, 255);

    private static final int colorBarLeftDurability = ARGB.color(255, 20, 124, 0);
    private static final int colorBarRightDurability = ARGB.color(255, 115, 255, 89);

    private static final int colorBarLeftDepleted = ARGB.color(255, 122, 0, 0);
    private static final int colorBarRightDepleted = ARGB.color(255, 255, 27, 27);

    public static void render(GuiGraphicsExtractor graphics, int level, int xPosition, int yPosition, int offset, boolean shadow,
                              int left, int right, boolean doDepletedColor) {
        if (doDepletedColor && level <= BAR_W / 4) {
            left = colorBarLeftDepleted;
            right = colorBarRightDepleted;
        }

        int x = xPosition + 2;
        int y = yPosition + 13 - offset;
        graphics.nextStratum();
        graphics.fill(x, y, x + BAR_W + 1, y + (shadow ? 2 : 1), colorShadow);
        if (level > 0) {
            RenderUtil.fillHorizontalGradient(graphics, x, y, x + Math.min(level, BAR_W + 1), y + 1, left, right);
        }
    }

    public static void renderBarsTool(GuiGraphicsExtractor graphics, IGTTool tool, ItemStack stack, int xPosition,
                                      int yPosition) {
        boolean renderedDurability = false;
        CompoundTag tag = com.gregtechceu.gtceu.api.item.data.ItemStackData.read(stack);
        if (!tag.getBooleanOr(ToolHelper.UNBREAKABLE_KEY, false)) {
            renderedDurability = renderDurabilityBar(graphics, stack.getBarWidth(), xPosition, yPosition);
        }
        if (tool.isElectric()) {
            renderElectricBar(graphics, tool.getCharge(stack), tool.getMaxCharge(stack), xPosition, yPosition,
                    renderedDurability);
        }
    }

    public static boolean renderElectricBar(GuiGraphicsExtractor graphics, long charge, long maxCharge, int xPosition,
                                            int yPosition, boolean renderedDurability) {
        if (charge > 0 && maxCharge > 0) {
            int level = Math.round(charge * 13.0F / maxCharge);
            render(graphics, level, xPosition, yPosition, renderedDurability ? 2 : 0, true, colorBarLeftEnergy,
                    colorBarRightEnergy, true);
            return true;
        }
        return false;
    }

    public static boolean renderDurabilityBar(GuiGraphicsExtractor graphics, ItemStack stack, IDurabilityBar manager,
                                              int xPosition, int yPosition) {
        float level = manager.getDurabilityForDisplay(stack);
        if (level == 0.0 && !manager.showEmptyBar(stack)) return false;
        if (level == 1.0 && !manager.showFullBar(stack)) return false;
        IntIntPair colors = manager.getDurabilityColorsForDisplay(stack);
        boolean doDepletedColor = manager.doDamagedStateColors(stack);
        int left = colors != null ? colors.leftInt() : colorBarLeftDurability;
        int right = colors != null ? colors.rightInt() : colorBarRightDurability;
        render(graphics, manager.getBarWidth(stack), xPosition, yPosition, 0, true, left, right, doDepletedColor);
        return true;
    }

    private static boolean renderDurabilityBar(GuiGraphicsExtractor graphics, int level, int xPosition, int yPosition) {
        render(graphics, level, xPosition, yPosition, 0, true, colorBarLeftDurability, colorBarRightDurability, true);
        return true;
    }

    private ToolChargeBarRenderer() {}
}
