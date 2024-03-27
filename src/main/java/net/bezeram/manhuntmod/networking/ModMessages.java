package net.bezeram.manhuntmod.networking;

import com.mojang.serialization.Decoder;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;
import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.networking.packets.HunterCompassGetPosC2SPacket;
import net.bezeram.manhuntmod.networking.packets.HunterCompassGetPosS2CPacket;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
import net.bezeram.manhuntmod.networking.packets.ResetClientDataS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

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

		net.messageBuilder(HunterCompassGetPosS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(HunterCompassGetPosS2CPacket::new)
				.encoder(HunterCompassGetPosS2CPacket::toBytes)
				.consumerMainThread(HunterCompassGetPosS2CPacket::handle)
				.add();

		net.messageBuilder(ResetClientDataS2CPacket.class, ID(), NetworkDirection.PLAY_TO_CLIENT)
				.decoder(ResetClientDataS2CPacket::new)
				.encoder(ResetClientDataS2CPacket::toBytes)
				.consumerMainThread(ResetClientDataS2CPacket::handle)
				.add();
	}

	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}

	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}
