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
    }

    public UpdateGameStateS2CPacket(boolean inSession) {
        this.inSession = inSession;
    }

    public void toBytes(FriendlyByteBuf buff) {
        buff.writeBoolean(inSession);
    }

    public static void handle(final UpdateGameStateS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE

                // If the game shuts down, reset client data
                if (ClientData.get().isGameInSession() != message.inSession)
                    ClientData.get().reset(message.inSession);
                else
                    ClientData.get().setGameSession(message.inSession);
            });
        });
        context.setPacketHandled(true);
    }

    private final boolean inSession;
}
