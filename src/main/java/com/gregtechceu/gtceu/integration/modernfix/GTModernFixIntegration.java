package com.gregtechceu.gtceu.integration.modernfix;

import com.gregtechceu.gtceu.client.util.AssetEventListener;
import com.gregtechceu.gtceu.client.util.ModelEventHelper;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.neoforged.neoforge.client.event.ModelEvent;

import lombok.Getter;
import org.embeddedt.modernfix.ModernFixClient;
import org.embeddedt.modernfix.api.entrypoint.ModernFixClientIntegration;
import org.jetbrains.annotations.ApiStatus;

public class GTModernFixIntegration implements ModernFixClientIntegration {

    private static GTModernFixIntegration INSTANCE = null;
    @Getter
    private static boolean dynamicResourcesEnabled = false;

    @ApiStatus.Internal
    public GTModernFixIntegration() {
        INSTANCE = this;
    }

    public static void setAsLast() {
        if (INSTANCE != null) {
            ModernFixClient.CLIENT_INTEGRATIONS.remove(INSTANCE);
        } else {
            INSTANCE = new GTModernFixIntegration();
        }
        ModernFixClient.CLIENT_INTEGRATIONS.add(INSTANCE);
    }

    @Override
    public void onDynamicResourcesStatusChange(boolean enabled) {
        dynamicResourcesEnabled = enabled;
    }

    /**
     * Apply the shared model replacements when ModernFix owns dynamic resource reloads.
     * Its pinned integration callback uses removed 1.20 model types, while NeoForge's
     * baking-result event still exposes the complete 26.2 block-state model map.
     */
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!dynamicResourcesEnabled) return;

        var models = event.getBakingResult().blockStateModels();
        for (var entry : models.entrySet()) {
            BlockStateModel model = entry.getValue();
            for (var listener : ModelEventHelper.EVENT_LISTENERS) {
                if (!(listener.listener() instanceof AssetEventListener.BlockStateModelReplacement modelReplacement)) continue;
                model = modelReplacement.modifyBlockStateModel(entry.getKey(), model);
            }
            entry.setValue(model);
        }
    }
}
