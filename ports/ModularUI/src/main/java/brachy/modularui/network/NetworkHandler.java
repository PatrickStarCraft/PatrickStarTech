package brachy.modularui.network;

import brachy.modularui.network.packets.CloseAllGuisPacket;
import brachy.modularui.network.packets.CloseGuiPacket;
import brachy.modularui.network.packets.OpenGuiPacket;
import brachy.modularui.network.packets.ReopenGuiPacket;
import brachy.modularui.network.packets.SyncHandlerPacket;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    public static final String NETWORK_VERSION = "1.0.0";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        registrar.playBidirectional(OpenGuiPacket.TYPE, OpenGuiPacket.CODEC,
                OpenGuiPacket::execute, OpenGuiPacket::execute);
        registrar.playBidirectional(SyncHandlerPacket.TYPE, SyncHandlerPacket.CODEC,
                SyncHandlerPacket::execute, SyncHandlerPacket::execute);
        registrar.playBidirectional(CloseAllGuisPacket.TYPE, CloseAllGuisPacket.CODEC,
                CloseAllGuisPacket::execute, CloseAllGuisPacket::execute);
        registrar.playBidirectional(CloseGuiPacket.TYPE, CloseGuiPacket.CODEC,
                CloseGuiPacket::execute, CloseGuiPacket::execute);
        registrar.playBidirectional(ReopenGuiPacket.TYPE, ReopenGuiPacket.CODEC,
                ReopenGuiPacket::execute, ReopenGuiPacket::execute);
    }
}
