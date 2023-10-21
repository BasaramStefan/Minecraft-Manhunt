package net.bezeram.manhuntmod.item.custom;

import net.bezeram.manhuntmod.game_manager.Game;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class HunterCompassItem extends Item {
	/**
	 * <p>TargetPlayer : int</p>
	 * Represents the id of the target player.<br>
	 * The id is used in the Game's data where the players are stored.
	 */
	public static final String TAG_TARGET_PLAYER = "TargetPlayer";

	/**
	 * <p>TargetTracked : bool</p>
	 * If true, the player is tracked in real time and their current position is used.<br>
	 * Else, the player's last position in the current dimension is used (can be null).
	 */
	public static final String TAG_TARGET_TRACKED = "TargetTracked";

	public HunterCompassItem(Properties properties) {
		super(properties);
	}

	public static boolean isCompassTracking(ItemStack itemStack) {
		CompoundTag tag = itemStack.getTag();
		return tag != null && tag.contains(TAG_TARGET_TRACKED) && tag.getBoolean(TAG_TARGET_TRACKED);
	}

	public static void addTags(Level level, CompoundTag tag) {
		if (level.isClientSide() || !Game.isInSession())
			return;

		Game.PlayersList playersList = Game.get().getPlayers();
		ServerPlayer firstRunner = Game.get().getRunnersArray()[0];

		String runnerName = firstRunner.getName().getString();
		int runnerID = playersList.getIDByName(runnerName);
		tag.putInt(TAG_TARGET_PLAYER, runnerID);

		ResourceKey<Level> dimension = level.dimension();
		int runnerDimensionID = Game.getDimensionID(firstRunner.getLevel().dimension());
		int levelDimensionID = Game.getDimensionID(dimension);
		tag.putBoolean(TAG_TARGET_TRACKED, runnerDimensionID == levelDimensionID);
	}

	public static void removeTags(Level level, CompoundTag tag) {
		if (level.isClientSide() || Game.isInSession())
			return;

		// If game has ended, remove the compass tags
		if (tag.contains(TAG_TARGET_PLAYER))
			tag.remove(TAG_TARGET_PLAYER);
		if (tag.contains(TAG_TARGET_TRACKED))
			tag.remove(TAG_TARGET_TRACKED);
	}

	@Override
	@ParametersAreNonnullByDefault
	public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		// TODO: This function crashes the client
		ItemStack itemUsed = player.getItemInHand(interactionHand);
		if (level.isClientSide()) {
			boolean shiftPressed = Screen.hasShiftDown();
			boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
			player.displayClientMessage(Component.literal("Sending packet"), false);
			ModMessages.sendToServer(new HunterCompassUseC2SPacket(shiftPressed, mainHand));
			return InteractionResultHolder.success(itemUsed);
		}

		return InteractionResultHolder.success(itemUsed);
	}
}
