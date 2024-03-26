package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HunterCompassGetPosS2CPacket {

    public HunterCompassGetPosS2CPacket(FriendlyByteBuf buff) {
        this.targetX = buff.readInt();
        this.targetZ = buff.readInt();
    }

    public HunterCompassGetPosS2CPacket(int targetX, int targetZ) {
        this.targetX = targetX;
        this.targetZ = targetZ;
    }

    public void toBytes(FriendlyByteBuf buff) {
        buff.writeInt(targetX);
        buff.writeInt(targetZ);
    }

    public static void handle(final HunterCompassGetPosS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE
                ClientData.Compass compassData = ClientData.get().getCompassData();
                compassData.targetX = message.targetX;
                compassData.targetZ = message.targetZ;
            });
        });
        context.setPacketHandled(true);
    }

    private final int targetX;
    private final int targetZ;
}
