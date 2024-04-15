package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class UpdatePortalRespawnS2CPacket {

    public UpdatePortalRespawnS2CPacket(FriendlyByteBuf buff) {
        this.portalRespawnCoords = buff.readBlockPos();
    }
    public UpdatePortalRespawnS2CPacket(final BlockPos portalRespawnCoords) {
        this.portalRespawnCoords = portalRespawnCoords;
    }

    public void toBytes(FriendlyByteBuf buff) {
        buff.writeBlockPos(portalRespawnCoords);
    }

    public static void handle(final UpdatePortalRespawnS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE

                // Modify in ClientData
                ClientData.get().setPortalRespawnCoords(message.portalRespawnCoords);
            });
        });
        context.setPacketHandled(true);
    }

    private final BlockPos portalRespawnCoords;
}
