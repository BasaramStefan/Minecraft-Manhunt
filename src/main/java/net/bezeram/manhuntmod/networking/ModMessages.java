package net.bezeram.manhuntmod.networking;

import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.networking.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Debug;

public class ModMessages {
	private static SimpleChannel INSTANCE;

	private static int packetID = 0;
	private static int ID() { return packetID++; }

	public static void register() {
		SimpleChannel net = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(ManhuntMod.MOD_ID, "messages"))
				.networkProtocolVersion(() -> "1.0")
				.clientAcceptedVersions(s -> true)
				.serverAcceptedVersions(s -> true)
				.simpleChannel();

		INSTANCE = net;

		net.messageBuilder(HunterCompassUseC2SPacket.class, ID(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(HunterCompassUseC2SPacket::new)
				.encoder(HunterCompassUseC2SPacket::toBytes)
				.consumerMainThread(HunterCompassUseC2SPacket::handle)
				.add();

		net.messageBuilder(HunterCompassGetPosC2SPacket.class, ID(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(HunterCompassGetPosC2SPacket::new)
				.encoder(HunterCompassGetPosC2SPacket::toBytes)
				.consumerMainThread(HunterCompassGetPosC2SPacket::handle)
				.add();

		net.messageBuilder(PortalRespawnerC2SPacket.class, ID(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(PortalRespawnerC2SPacket::new)
				.encoder(PortalRespawnerC2SPacket::toBytes)
				.consumerMainThread(PortalRespawnerC2SPacket::handle)
				.add();

		net.messageBuilder(HunterCompassGetPosS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(HunterCompassGetPosS2CPacket::new)
				.encoder(HunterCompassGetPosS2CPacket::toBytes)
				.consumerMainThread(HunterCompassGetPosS2CPacket::handle)
				.add();

		net.messageBuilder(UpdateGameStateS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(UpdateGameStateS2CPacket::new)
				.encoder(UpdateGameStateS2CPacket::toBytes)
				.consumerMainThread(UpdateGameStateS2CPacket::handle)
				.add();

		net.messageBuilder(UpdatePortalRespawnS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(UpdatePortalRespawnS2CPacket::new)
				.encoder(UpdatePortalRespawnS2CPacket::toBytes)
				.consumerMainThread(UpdatePortalRespawnS2CPacket::handle)
				.add();

		net.messageBuilder(PortalRespawnSetAcknowledgeS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(PortalRespawnSetAcknowledgeS2CPacket::new)
				.encoder(PortalRespawnSetAcknowledgeS2CPacket::toBytes)
				.consumerMainThread(PortalRespawnSetAcknowledgeS2CPacket::handle)
				.add();

		net.messageBuilder(DebugPrintClientDataS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(DebugPrintClientDataS2CPacket::new)
				.encoder(DebugPrintClientDataS2CPacket::toBytes)
				.consumerMainThread(DebugPrintClientDataS2CPacket::handle)
				.add();
	}

	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}

	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}
