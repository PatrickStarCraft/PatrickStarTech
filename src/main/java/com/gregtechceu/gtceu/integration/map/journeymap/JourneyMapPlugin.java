package com.gregtechceu.gtceu.integration.map.journeymap;

import com.gregtechceu.gtceu.GTCEu;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import lombok.Getter;

@journeymap.api.v2.common.JourneyMapPlugin(apiVersion = "26.2-2.0.0")
public class JourneyMapPlugin implements IClientPlugin {

    @Getter
    private static boolean active = false;

    @Getter
    private static IClientAPI jmApi;

    @Getter
    private static JourneymapOptions options;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        active = true;
        jmApi = jmClientApi;
        ClientEventRegistry.OPTIONS_REGISTRY_EVENT.subscribe(GTCEu.MOD_ID,
                event -> options = new JourneymapOptions());
        JourneymapEventListener.init();
    }

    @Override
    public String getModId() {
        return GTCEu.MOD_ID;
    }

}
