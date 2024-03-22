package net.bezeram.manhuntmod.item.custom;

import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.players.MAIDArray;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Hashtable;
import java.util.UUID;

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

	@Override
	public boolean isFoil(ItemStack itemStack) { return isCompassTracking(itemStack) || super.isFoil(itemStack); }

	public static void addOrUpdateTags(Level compassLevel, CompoundTag tag) {
		if (compassLevel.isClientSide || !Game.inSession())
			return;
		MAIDArray MAIDArray = Game.get().getPlayerData().getPlayerArray();

		if (!tag.contains(TAG_TARGET_PLAYER)) {
			tag.putInt(TAG_TARGET_PLAYER, 0);
		}

		ServerPlayer targetPlayer = MAIDArray.getPlayer(tag.getInt(TAG_TARGET_PLAYER));
//		int runnerDimensionID = Game.getDimensionID(targetPlayer.getLevel().dimension());
//		int levelDimensionID = Game.getDimensionID(compassLevel.dimension());
//		tag.putBoolean(TAG_TARGET_TRACKED, runnerDimensionID == levelDimensionID);
	}

	public static void removeTags(Level level, CompoundTag tag) {
		if (level.isClientSide || Game.inSession())
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

	public static void onPlayerChangeDimension(ServerPlayer traveler, Level newLevel) {
		if (!Game.inSession())
			return;

		// If the current compass checked belongs to the traveler, update its dimension before updating the tag
		UUID travelerUUID = traveler.getUUID();
//		int newDimensionID = Game.getDimensionID(newLevel.dimension());
		if (Game.get().getPlayerData().isHunter(traveler)) {
			for (ItemStack itemStack : traveler.getInventory().items) {
				if (itemStack.getItem() instanceof HunterCompassItem) {
					HunterCompassItem.addOrUpdateTags(newLevel, itemStack.getOrCreateTag());
					StaticCompassType value = allCompasses.get(travelerUUID);
					value.compassRef = itemStack;
//					value.dimensionID = newDimensionID;
				}
			}
		}

		// Iterate over all compasses and update their PLAYER_TRACKED tag.
		for (UUID key : allCompasses.keySet()) {
			StaticCompassType value = allCompasses.get(key);

			HunterCompassItem.addOrUpdateTags(newLevel, value.compassRef.getOrCreateTag());
			allCompasses.put(travelerUUID, value);
		}
	}

	public static void putGlobalCompass(UUID playerUUID, ItemStack compass, int dimensionID) {
		if (allCompasses.containsKey(playerUUID)) {
			StaticCompassType value = allCompasses.get(playerUUID);
			value.compassRef = compass;
			value.dimensionID = dimensionID;
			allCompasses.put(playerUUID, value);
		}

		allCompasses.put(playerUUID, new StaticCompassType(compass, dimensionID));
	}

	public static void updateGlobalCompass(UUID playerUUID, ItemStack compass, int dimensionID) {
		StaticCompassType value = allCompasses.get(playerUUID);
		value.compassRef = compass;
		value.dimensionID = dimensionID;
	}

	public static void updateGlobalCompass(UUID playerUUID, ItemStack compass) {
		StaticCompassType value = allCompasses.get(playerUUID);
		value.compassRef = compass;
	}

	public static void updateGlobalCompass(UUID playerUUID, int dimensionID) {
		StaticCompassType value = allCompasses.get(playerUUID);
		value.dimensionID = dimensionID;
	}

	public static void removeCompassRef(UUID playerUUID) {
		allCompasses.remove(playerUUID);
	}

	public static void clearCompassList() {
		allCompasses.clear();
	}

	// TODO: Change this, it just does not work when a player is changing dimensions. Think. Refactor
	static class StaticCompassType {
		StaticCompassType(ItemStack compassRef, int dimensionID) {
			this.compassRef = compassRef;
			this.dimensionID = dimensionID;
		}

		ItemStack compassRef;
		int dimensionID;
	}

	private static final Hashtable<UUID, StaticCompassType> allCompasses = new Hashtable<>();
}
