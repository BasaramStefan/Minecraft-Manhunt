package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static net.bezeram.manhuntmod.item.custom.HunterCompassItem.getPlayerPosition;

public class HunterCompassGetPosC2SPacket {

    public HunterCompassGetPosC2SPacket(FriendlyByteBuf buff) {
        this.MAID = buff.readInt();
        this.isTracking = buff.readBoolean();
    }

    public HunterCompassGetPosC2SPacket(int MAID, boolean isTracking) {
        this.MAID = MAID;
        this.isTracking = isTracking;
    }

    public void toBytes(FriendlyByteBuf buff) {
        buff.writeInt(MAID);
        buff.writeBoolean(isTracking);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // SERVER SIDE
            ServerPlayer hunter = context.getSender();
            ServerLevel compassLevel = hunter.getLevel();
            ServerPlayer target = Game.get().getPlayer(MAID);

            BlockPos playerPos = getPlayerPosition(isTracking, compassLevel, target);
            ModMessages.sendToPlayer(new HunterCompassGetPosS2CPacket(playerPos.getX(), playerPos.getZ()), hunter);
        });
    }

    private final int MAID;
    private final boolean isTracking;
}
