package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.client.util.RenderUtil;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import brachy.modularui.drawable.text.FontRenderHelper;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.viewport.GuiContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    private ItemStack tooltipStack;

    @WrapMethod(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V")
    private void gtceu$renderResearchItemContent(@Nullable LivingEntity entity, @Nullable Level level,
                                                 ItemStack stack, int x, int y, int seed,
                                                 Operation<Void> original) {
        if (!RenderUtil.renderResearchItemContent((GuiGraphicsExtractor) (Object) this, original,
                entity, level, stack, x, y, seed)) {
            original.call(entity, level, stack, x, y, seed);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V")
    private void gtceu$replaceComponentTooltip(Font font, List<Component> lines,
                                               Optional<TooltipComponent> image, int mouseX, int mouseY,
                                               Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, asFormattedText(lines), image, mouseX, mouseY, this.tooltipStack)) {
            original.call(font, lines, image, mouseX, mouseY);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/world/item/ItemStack;II)V")
    private void gtceu$replaceStackTooltip(Font font, List<Component> lines,
                                           Optional<TooltipComponent> image, ItemStack stack,
                                           int mouseX, int mouseY, Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, asFormattedText(lines), image, mouseX, mouseY, stack)) {
            original.call(font, lines, image, stack, mouseX, mouseY);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;Lnet/minecraft/world/item/ItemStack;IILnet/minecraft/resources/Identifier;)V")
    private void gtceu$replaceStyledStackTooltip(Font font, List<Component> lines,
                                                  Optional<TooltipComponent> image, ItemStack stack,
                                                  int mouseX, int mouseY, Identifier style,
                                                  Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, asFormattedText(lines), image, mouseX, mouseY, stack)) {
            original.call(font, lines, image, stack, mouseX, mouseY, style);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V")
    private void gtceu$replaceStyledComponentTooltip(Font font, List<Component> lines,
                                                      Optional<TooltipComponent> image, int mouseX, int mouseY,
                                                      Identifier style,
                                                      Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, asFormattedText(lines), image, mouseX, mouseY, this.tooltipStack)) {
            original.call(font, lines, image, mouseX, mouseY, style);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V")
    private void gtceu$replaceFormattedSequenceTooltip(Font font, List<FormattedCharSequence> lines,
                                                        int mouseX, int mouseY, Operation<Void> original) {
        List<FormattedText> text = lines.stream()
                .map(FontRenderHelper::getComponentFromCharSequence)
                .collect(Collectors.toList());
        if (!gtceu$drawRichTooltip(font, text, Optional.empty(), mouseX, mouseY, this.tooltipStack)) {
            original.call(font, lines, mouseX, mouseY);
        }
    }

    @WrapMethod(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZ)V")
    private void gtceu$replacePositionedTooltip(Font font, List<FormattedCharSequence> lines,
                                                ClientTooltipPositioner positioner, int mouseX, int mouseY,
                                                boolean replaceExisting, Operation<Void> original) {
        List<FormattedText> text = lines.stream()
                .map(FontRenderHelper::getComponentFromCharSequence)
                .collect(Collectors.toList());
        if (!gtceu$drawRichTooltip(font, text, Optional.empty(), mouseX, mouseY, this.tooltipStack)) {
            original.call(font, lines, positioner, mouseX, mouseY, replaceExisting);
        }
    }

    @WrapMethod(method = "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V")
    private void gtceu$replaceComponentListTooltip(Font font, List<Component> lines, int mouseX, int mouseY,
                                                   @Nullable Identifier style,
                                                   Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, asFormattedText(lines), Optional.empty(), mouseX, mouseY, this.tooltipStack)) {
            original.call(font, lines, mouseX, mouseY, style);
        }
    }

    @WrapMethod(method = "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/world/item/ItemStack;)V")
    private void gtceu$replaceFormattedTextTooltip(Font font, List<FormattedText> lines,
                                                   int mouseX, int mouseY, ItemStack stack,
                                                   Operation<Void> original) {
        if (!gtceu$drawRichTooltip(font, lines, Optional.empty(), mouseX, mouseY, stack)) {
            original.call(font, lines, mouseX, mouseY, stack);
        }
    }

    @Unique
    private boolean gtceu$drawRichTooltip(Font font, List<? extends FormattedText> lines,
                                          Optional<TooltipComponent> tooltipComponent,
                                          int mouseX, int mouseY, @Nullable ItemStack stack) {
        if (!ConfigHolder.INSTANCE.client.ui.replaceVanillaTooltips || lines.isEmpty()) return false;

        RichTooltip tooltip = new RichTooltip();
        tooltip.parent(area -> RichTooltip.findIngredientArea(area, mouseX, mouseY));
        tooltip.add((Component) lines.get(0)).newLine();
        // Vanilla inserts bundle contents after the title.
        tooltipComponent.ifPresent(component -> tooltip.addLine(component.toString()));
        if (stack != null && !stack.isEmpty()) tooltip.spaceLine();
        for (int i = 1; i < lines.size(); i++) tooltip.add((Component) lines.get(i)).newLine();

        GuiContext context = GuiContext.getDefault();
        GuiGraphicsExtractor previousGraphics = context.getGraphics();
        context.setOverrideFont(font);
        context.setGraphics((GuiGraphicsExtractor) (Object) this);
        try {
            tooltip.draw(context, stack);
        } finally {
            context.setGraphics(previousGraphics);
            context.setOverrideFont(null);
        }
        return true;
    }

    @Unique
    private static List<FormattedText> asFormattedText(List<Component> lines) {
        return new ArrayList<>(lines);
    }
}
