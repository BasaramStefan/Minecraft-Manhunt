package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.bezeram.manhuntmod.game.Game;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PortalRespawnSetAcknowledgeS2CPacket {

    public PortalRespawnSetAcknowledgeS2CPacket(FriendlyByteBuf ignored) {}
    public PortalRespawnSetAcknowledgeS2CPacket() {}

    public void toBytes(FriendlyByteBuf ignored) {}

    public static void handle(final PortalRespawnSetAcknowledgeS2CPacket ignored,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE

                // Modify in ClientData
                ClientData.get().respawnPointAcknowledged();
                Game.LOG("Portal Respawn change acknowledged");
            });
        });
        context.setPacketHandled(true);
    }
}
