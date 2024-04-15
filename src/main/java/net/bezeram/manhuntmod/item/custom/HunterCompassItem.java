package net.bezeram.manhuntmod.item.custom;

import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.players.CompassArray;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassUseC2SPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

public class HunterCompassItem extends Item {
	/**
	 * <p>TargetPlayer : int</p>
	 * Represents the id of the target player.<br>
	 * The id is used in the Game's data where the players are stored.
	 */
	public static final String TAG_TARGET_PLAYER = "TargetPlayer";

	/**
	 * <p>TargetTracked : boolean</p>
	 * If true, the player is tracked in real time and their current position is used.<br>
	 * Else, the player's last position in the current dimension is used (can be null).
	 */
	public static final String TAG_TARGET_TRACKING = "TargetTracked";

	public HunterCompassItem(final Properties properties) {
		super(properties);
	}

	public static boolean isCompassTracking(final CompoundTag tag) {
		return tag != null && tag.contains(TAG_TARGET_TRACKING) && tag.getBoolean(TAG_TARGET_TRACKING);
	}

	@Override
	@ParametersAreNonnullByDefault
	public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
		ItemStack itemUsed = player.getItemInHand(interactionHand);
		if (level.isClientSide) {
			boolean shiftPressed = Screen.hasShiftDown();
			boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
			ModMessages.sendToServer(new HunterCompassUseC2SPacket(shiftPressed, mainHand));
			return InteractionResultHolder.success(itemUsed);
		}

		return InteractionResultHolder.fail(itemUsed);
	}

	@Override
	@ParametersAreNonnullByDefault
	public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (level.isClientSide())
			return;

		boolean prevValue = itemStack.getOrCreateTag().getBoolean(TAG_TARGET_TRACKING);
		addOrUpdateTags(level, itemStack.getTag());
		boolean currentValue = itemStack.getOrCreateTag().getBoolean(TAG_TARGET_TRACKING);
		if (prevValue != currentValue) {
			SoundEvent sound = (currentValue) ?  SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE;
			BlockPos soundPos = new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
			level.playSound(null, soundPos, sound, SoundSource.PLAYERS, 1.f, 1.f);
		}
	}

	/**
	 * Adds the target player tag (int: MAID) if it doesn't exist and updates the tracking boolean
	 * depending on if the compass is in the same or different dimension as the player tracked
	 * @param compassLevel the compass level
	 * @param tag compass tags
	 */
	public static void addOrUpdateTags(final Level compassLevel, CompoundTag tag) {
		if (compassLevel.isClientSide || !Game.inSession())
			return;
		if (!tag.contains(TAG_TARGET_PLAYER))
			tag.putInt(TAG_TARGET_PLAYER, 0);

		CompassArray compassArray = Game.get().getPlayerData().getPlayerArray();
		try {
			ServerPlayer targetPlayer = compassArray.getPlayer(tag.getInt(TAG_TARGET_PLAYER));

			DimensionID runnerDim = Game.getDimensionID(targetPlayer.getLevel().dimension());
			DimensionID compassDim = Game.getDimensionID(compassLevel.dimension());
			tag.putBoolean(TAG_TARGET_TRACKING, runnerDim == compassDim);
		} catch (NullPointerException ignored) {}
	}

	/**
	 * Gets the live coords of the player if the compass is actively tracking, or gets the latest coords in that
	 * dimension.<br>
	 * Used by the CompassItemPropertyFunction class
 	 * @param compassLevel compass level
	 * @param tag compass tags
	 * @return coords with a dimension
	 */
	public static @Nullable GlobalPos getPlayerPosition(final ServerLevel compassLevel, final CompoundTag tag) {
		if (!Game.inSession()) {
			return null;
		}
		if (!tag.contains(TAG_TARGET_PLAYER))
			tag.putInt(TAG_TARGET_PLAYER, 0);
		int MAID = tag.getInt(TAG_TARGET_PLAYER);

		try {
			ServerPlayer target = Game.get().getPlayer(MAID);
			BlockPos blockPos = getPlayerPosition(isCompassTracking(tag), compassLevel, target);
			if (blockPos == null)
				return null;
			return GlobalPos.of(compassLevel.dimension(), blockPos);
		} catch (Exception ignored) {
			return null;
		}
	}

	/**
	 * Get the position of the target player
	 * @param isTracking is the compass tracking the player live
	 * @param compassLevel compass level
	 * @param target targeted player
	 * @return coordinates along with compass dimension
	 */
	public static BlockPos getPlayerPosition(boolean isTracking, ServerLevel compassLevel,
	                                          final ServerPlayer target) {
		// Live coords
		try {
			if (isTracking) {
				Vec3 pos = target.getPosition(1);
				return new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
			}
		} catch (NullPointerException e) {
			return null;
		}

		// Get the latest known coords in the compass's dimension of the target player
		Vec3 pos = Game.get().getPlayerData().getCoords(compassLevel.dimension()).get(target.getUUID());
		if (pos == null)
			return null;
		return new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
	}

	@Override
	@ParametersAreNonnullByDefault
	public boolean isFoil(ItemStack itemStack) { return isCompassTracking(itemStack.getOrCreateTag());}

	public static void removeTags(final Level level, final CompoundTag tag) {
		if (level.isClientSide || Game.inSession())
			return;

		// If game has ended, remove the compass tags
		if (tag.contains(TAG_TARGET_PLAYER))
			tag.remove(TAG_TARGET_PLAYER);
		if (tag.contains(TAG_TARGET_TRACKING))
			tag.remove(TAG_TARGET_TRACKING);
	}
}
