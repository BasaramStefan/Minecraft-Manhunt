package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DebugPrintClientDataS2CPacket {

    public DebugPrintClientDataS2CPacket(FriendlyByteBuf ignored) {}
    public DebugPrintClientDataS2CPacket() {}
    public void toBytes(FriendlyByteBuf ignored) {}

    public static void handle(final DebugPrintClientDataS2CPacket ignored, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // CLIENT SIDE
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                System.out.println("Player not found");
                return;
            }

            ClientData.HunterCompass compassData = ClientData.get().getHunterCompass();
            BlockPos portalRespawnCoords = ClientData.get().getPortalRespawnCoords();
            boolean isGameInSession = ClientData.get().isGameInSession();

            player.displayClientMessage(Component.literal("[Hunter Compass]: "
                    + "Target Player coords: " + "(X, Z) = " + compassData.toString()
                    + " [Portal Respawn]: " + ((portalRespawnCoords != null) ?
                    ("(X, Z) = (" + portalRespawnCoords.getX() + ", " + portalRespawnCoords.getZ() + ")") : "NULL")
                    + " [GameState]: " + ((isGameInSession) ? "active" : "inactive")
            ), false);
        }));
        context.setPacketHandled(true);
    }
}
