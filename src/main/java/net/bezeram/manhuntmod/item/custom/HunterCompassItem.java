package net.bezeram.manhuntmod.item.custom;

import net.bezeram.manhuntmod.game_manager.Game;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Hashtable;

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

	public static void addOrUpdateTags(Level compassLevel, CompoundTag tag) {
		if (compassLevel.isClientSide || !Game.isInSession())
			return;
		Game.PlayersList playersList = Game.get().getPlayers();

		if (!tag.contains(TAG_TARGET_PLAYER)) {
			tag.putInt(TAG_TARGET_PLAYER, 0);
		}

		ServerPlayer targetPlayer = playersList.getPlayer(tag.getInt(TAG_TARGET_PLAYER));
		int runnerDimensionID = Game.getDimensionID(targetPlayer.getLevel().dimension());
		int levelDimensionID = Game.getDimensionID(compassLevel.dimension());
		tag.putBoolean(TAG_TARGET_TRACKED, runnerDimensionID == levelDimensionID);
	}

	public static void removeTags(Level level, CompoundTag tag) {
		if (level.isClientSide || Game.isInSession())
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
		ItemStack itemUsed = player.getItemInHand(interactionHand);
		if (level.isClientSide) {
			boolean shiftPressed = Screen.hasShiftDown();
			boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
			ModMessages.sendToServer(new HunterCompassUseC2SPacket(shiftPressed, mainHand));
			return InteractionResultHolder.fail(itemUsed);
		}

		return InteractionResultHolder.fail(itemUsed);
	}

	public static void onPlayerChangeDimension(String playerName, Level level) {
		if (!Game.isInSession())
			return;

		int dimensionID = Game.getDimensionID(level.dimension());
		int playerID = Game.get().getPlayers().getIDByName(playerName);
		if (playerID != -1) {
			for (ItemStack key : allCompasses.keySet()) {
				HunterCompassItem.addOrUpdateTags(level, key.getOrCreateTag());
				HunterCompassItem.addOrUpdateCompass(key, dimensionID);
			}
		}
	}

	public static void addOrUpdateCompass(ItemStack itemStack, int dimensionID) {
		allCompasses.put(itemStack, dimensionID);
	}

	public static void removeCompassRef(ItemStack itemStack) {
		allCompasses.remove(itemStack);
	}

	public static void clearCompassList() {
		allCompasses.clear();
	}

	// TODO: Change this, it just does not work when a player is changing dimensions. Think. Refactor
	private static final Hashtable<ItemStack, Integer> allCompasses = new Hashtable<>();
}
