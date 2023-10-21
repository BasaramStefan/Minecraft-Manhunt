package net.bezeram.manhuntmod.networking;

import com.mojang.serialization.Decoder;
import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
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
	}

	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}

	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}
