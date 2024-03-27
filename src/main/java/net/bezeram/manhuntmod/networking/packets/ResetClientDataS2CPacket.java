package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ResetClientDataS2CPacket {
    public ResetClientDataS2CPacket(FriendlyByteBuf friendlyByteBuf) {}
    public ResetClientDataS2CPacket() {}

    public void toBytes(FriendlyByteBuf friendlyByteBuf) {}

    public static void handle(final ResetClientDataS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // CLIENT SIDE
                ClientData.get().reset();
            });
        });
        context.setPacketHandled(true);
    }
}
