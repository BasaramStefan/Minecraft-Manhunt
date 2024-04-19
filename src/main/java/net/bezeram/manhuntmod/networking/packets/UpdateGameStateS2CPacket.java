package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateGameStateS2CPacket {

    public UpdateGameStateS2CPacket(FriendlyByteBuf buff) {
        this.inSession = buff.readBoolean();
        this.isEndLocked = buff.readBoolean();
    }

    public UpdateGameStateS2CPacket(boolean inSession, boolean isEndLocked) {
        this.inSession = inSession;
        this.isEndLocked = isEndLocked;
    }

    public void toBytes(FriendlyByteBuf buff) {
        buff.writeBoolean(inSession);
        buff.writeBoolean(isEndLocked);
    }

    public static void handle(final UpdateGameStateS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE

                // If the game state changes, reset client data
                if (ClientData.get().isGameInSession() != message.inSession)
                    ClientData.get().reset(message.inSession);
                else
                    ClientData.get().setEndLocked(message.isEndLocked);
            });
        });
        context.setPacketHandled(true);
    }

    private final boolean inSession;
    private final boolean isEndLocked;
}
