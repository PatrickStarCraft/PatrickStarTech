package com.gregtechceu.gtceu.client.renderer.item.decorator;

import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import brachy.modularui.drawable.GuiDraw;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Range;

/**
 * An Item Decorator to render fluid icons for items with {@link Capabilities.Fluid#ITEM}.
 * <p>
 * The fluid type count can be up to 4, set by {@link #setMaxRenderCount(int)}, 1 by default.
 *
 * @author Taskeren
 */
public class GTTankItemFluidPreview implements IItemDecorator {

    public static final GTTankItemFluidPreview DRUM = new GTTankItemFluidPreview() {

        @Override
        public boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack itemStack, int x, int y) {
            if (!ConfigHolder.INSTANCE.client.tankItemFluidPreview.drum) return false;
            return super.render(guiGraphics, font, itemStack, x, y);
        }
    };

    public static final GTTankItemFluidPreview QUANTUM_TANK = new GTTankItemFluidPreview() {

        @Override
        public boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack itemStack, int x, int y) {
            if (!ConfigHolder.INSTANCE.client.tankItemFluidPreview.quantumTank) return false;
            return super.render(guiGraphics, font, itemStack, x, y);
        }
    };

    /**
     * The fluid icon draw offset to the icon top-left, it should be in order of bottom-right, bottom-left, top-right,
     * top-left.
     */
    private static final float[][] OFFSET = { { 8, 8 }, { 0, 8 }, { 8, 0 }, { 0, 0 } };

    /**
     * The maximum count of fluids to be rendered, in range from 0 (render nothing) to 4.
     */
    @Getter
    @Range(from = 0, to = 4)
    private int maxRenderCount = 1;

    /**
     * If {@code true}, the fluid icon is rendered on top of the item.
     */
    @Getter
    @Setter
    private boolean renderOnTopOfItem = true;

    @Getter
    @Setter
    private boolean requireShiftKeyDown = false;

    public void setMaxRenderCount(int maxRenderCount) {
        if (maxRenderCount < 0 || maxRenderCount > 4) {
            throw new IllegalArgumentException("maxRenderCount must be between 0 and 4");
        }
        this.maxRenderCount = maxRenderCount;
    }

    @Override
    public boolean render(GuiGraphicsExtractor guiGraphics, Font font, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty()) {
            return false;
        }
        if (isRequireShiftKeyDown() && !GTUtil.isShiftDown()) {
            return false;
        }

        ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(itemStack).getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler == null) {
            return false;
        }

        if (isRenderOnTopOfItem()) {
            guiGraphics.nextStratum();
        }

        for (int index = 0, renderedCount = 0; index < fluidHandler.size() &&
                renderedCount < getMaxRenderCount(); index++) {
            FluidResource resource = fluidHandler.getResource(index);
            int amount = fluidHandler.getAmountAsInt(index);
            if (!resource.isEmpty() && amount > 0) {
                FluidStack fluidInTank = resource.toStack(amount);
                GuiDraw.drawFluidTexture(
                        guiGraphics,
                        fluidInTank,
                        x + OFFSET[renderedCount][0],
                        y + OFFSET[renderedCount][1],
                        8.0F,
                        8.0F, 0);
                renderedCount++;
            }
        }

        return true;
    }
}
