package com.gregtechceu.gtceu.utils.fakeplayer;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {

    private static final Connection DUMMY_CONNECTION = new Connection(PacketFlow.CLIENTBOUND);

    public FakeServerGamePacketListenerImpl(MinecraftServer server, ServerPlayer player) {
        super(server, DUMMY_CONNECTION, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    @Override
    public void tick() {}

    @Override
    public void resetPosition() {}

    @Override
    public void disconnect(Component message) {}

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {}

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {}

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {}

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {}

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {}

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {}

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {}

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {}

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {}

    @Override
    public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket packet) {}

    @Override
    public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket packet) {}

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {}

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {}

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {}

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {}

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {}

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {}

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {}

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) {}

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) {}

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {}

    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {}

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {}

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {}

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {}

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {}

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {}

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {}

    @Override
    public void onDisconnect(DisconnectionDetails details) {}

    @Override
    public void send(Packet<?> packet) {}

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {}

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {}

    @Override
    public void handleChat(ServerboundChatPacket packet) {}

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {}

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {}

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {}

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {}

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {}

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {}

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {}

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {}

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {}

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {}

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {}

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {}

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {}

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {}

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {}

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {}

    @Override
    public void teleport(PositionMoveRotation destination, Set<Relative> relativeSet) {}

    @Override
    public void ackBlockChangesUpTo(int sequence) {}

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket packet) {}

    @Override
    public void handleChatAck(ServerboundChatAckPacket packet) {}

    @Override
    public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound boundChatType) {}

    @Override
    public void sendDisguisedChatMessage(Component content, ChatType.Bound boundChatType) {}

    @Override
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {}
}
