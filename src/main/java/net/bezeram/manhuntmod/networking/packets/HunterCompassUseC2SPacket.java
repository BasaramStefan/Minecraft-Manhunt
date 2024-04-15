package net.bezeram.manhuntmod.networking.packets;

import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.players.CompassArray;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HunterCompassUseC2SPacket {

	public HunterCompassUseC2SPacket(FriendlyByteBuf buff) {
		this.shiftPressed = buff.readBoolean();
		this.mainHand = buff.readBoolean();
	}

	public HunterCompassUseC2SPacket(boolean shiftPressed, boolean mainHand) {
		this.shiftPressed = shiftPressed;
		this.mainHand = mainHand;
	}

	public void toBytes(FriendlyByteBuf buff) {
		buff.writeBoolean(shiftPressed);
		buff.writeBoolean(mainHand);
	}

	public void handle(Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		context.enqueueWork(() -> {
			// SERVER SIDE

			try {
				ServerPlayer player = context.getSender();
				assert player != null;

				InteractionHand interactionHand = (this.mainHand) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
				ServerLevel level = player.getLevel();
				ItemStack itemUsed = player.getItemInHand(interactionHand);

				if (!Game.inSession()) {
					player.sendSystemMessage(Component.literal("No game in session").withStyle(ChatFormatting.RED));
					HunterCompassItem.removeTags(level, itemUsed.getOrCreateTag());
					return;
				}

				BlockPos playerBlockPos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());

				level.playSound(null, playerBlockPos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS,
						1.0F,
						1.0F);

				HunterCompassItem.addOrUpdateTags(level, itemUsed.getOrCreateTag());
				CompoundTag tag = itemUsed.getOrCreateTag();
				int MAID = tag.getInt(HunterCompassItem.TAG_TARGET_PLAYER);
				CompassArray compassArray = Game.get().getPlayerData().getPlayerArray();

				if (!shiftPressed) {
					// Toggling to runner
					int newID = compassArray.cycleRunners(MAID);
					tag.putInt(HunterCompassItem.TAG_TARGET_PLAYER, newID);

					String newTargetName = compassArray.getPlayer(newID).getName().getString();
					itemUsed.setHoverName(Component.literal("Tracking " + newTargetName));
				}
				else {
					// Toggling to hunter
					int newID = compassArray.cycleHunters(MAID);
					// check if the player cycled to is the same player using the compass
					if (compassArray.samePlayer(player, newID)) {
						if (compassArray.getHunterCount() == 1) {
							// Only one hunter in the team -> cancel use
							return;
						}

						// Cycle again
						newID = compassArray.cycleHunters(newID);
					}
					tag.putInt(HunterCompassItem.TAG_TARGET_PLAYER, newID);

					ServerPlayer targetPlayer = compassArray.getPlayer(newID);
					if (targetPlayer == null) {
						itemUsed.setHoverName(Component.literal("Tracking NULL"));
						return;
					}

					String newTargetName = targetPlayer.getName().getString();
					itemUsed.setHoverName(Component.literal("Tracking " + newTargetName));
				}
			} catch (Exception ignored) {}
		});
		context.setPacketHandled(true);
	}

	private final boolean shiftPressed;
	private final boolean mainHand;
}
