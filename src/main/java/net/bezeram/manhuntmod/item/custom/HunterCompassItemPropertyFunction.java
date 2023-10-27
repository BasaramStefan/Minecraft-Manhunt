package net.bezeram.manhuntmod.item.custom;

import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.UUID;

//public class HunterCompassItemPropertyFunction implements ClampedItemPropertyFunction {
//	// Outside of Manhunt game
//	public HunterCompassItemPropertyFunction(Item.Properties properties) {
//		super(properties);
//
//		allCompasses.add(this);
//	}
//
//	public Item init(ServerPlayer[] runners, ServerPlayer[] hunters, Entity entity) {
//		this.runners = runners;
//		this.hunters = hunters;
//		this.entity = entity;
//		this.trackedPlayerName = runners[0].getName().getString();
//		this.trackedPlayerUUID = runners[0].getUUID();
//		return this;
//	}
//
//	// Checks if the current tracked player is in the same dimension as the hunter
//	public boolean isTrackingPlayer() {
//		return trackedPlayerUUID != null;
//	}
//
//	@Override
//	@ParametersAreNonnullByDefault
//	public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
//		ItemStack itemStack = player.getItemInHand(interactionHand);
//
//		if (Screen.hasShiftDown()) {
//			cycleHunters();
//		}
//		else {
//			cycleRunners();
//		}
//
//		itemStack.setHoverName(Component.literal(trackedPlayerName));
//
//		return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
//	}
//
//	public Vec3 getTargetPosition() {
//		return targetPosition;
//	}
//
//	public Vec3 update(ClientLevel clientLevel) {
//		// Outside of Manhunt game
//		if (trackedPlayerName == null)
//			return null;
//
//		// Tracked player is in another dimension
//		if (trackedPlayerUUID == null) {
//			ResourceKey<Level> dimension = clientLevel.dimension();
//			Game.PlayerLastLocations location = Game.PlayerLastLocations.getByDimension(dimension);
//
//			if (location != null)
//				targetPosition = location.getLastPosition(trackedPlayerName);
//			return targetPosition;
//		}
//
//		// Tracked player is in the same dimension
//		Player player = clientLevel.getPlayerByUUID(trackedPlayerUUID);
//		targetPosition = player.getPosition(1);
//
//		return targetPosition;
//	}
//
//	public static GlobalPos propertyUpdate(ClientLevel clientLevel, ItemStack itemStack) {
//		HunterCompassItem compass = (HunterCompassItem) itemStack.getItem();
//		if (compass.getTrackedPlayer() == null)
//			return null;
//
//		Vec3 updatedPosition = compass.update(clientLevel);
//
//		if (updatedPosition == null)
//			return null;
//
//		Vec3i blockCoords = new Vec3i((int)updatedPosition.x, (int)updatedPosition.y, (int)updatedPosition.z);
//		return GlobalPos.of(clientLevel.dimension(), new BlockPos(blockCoords));
//	}
//
//	public String getTrackedPlayer() {
//		return trackedPlayerName;
//	}
//
//	public void playerChangedDimension() {
//		if (trackedPlayerName == null)
//			return;
//
//		ServerLevel runnerLevel = Game.get().getServer().getPlayerList().getPlayerByName(trackedPlayerName).getLevel();
//		ResourceKey<Level> dimensionHunter = entity.getLevel().dimension();
//		ResourceKey<Level> dimensionRunner = runnerLevel.dimension();
//		if (dimensionRunner != dimensionHunter) {
//			trackedPlayerUUID = null;
//		}
//	}
//
//	public static void onPlayerChangedDimension() {
//		for (HunterCompassItem compass : allCompasses) {
//			compass.playerChangedDimension();
//		}
//	}
//
//	public String getUUIDString() {
//		return this.trackedPlayerUUID.toString();
//	}
//
//	public void cycleRunners() {
//		if (runners == null || runners.length == 0)
//			return;
//
//		// When switching teams, it makes more sense to flip back to the previous runner selected
//		if (trackingRunner) {
//			indexRunners = (indexRunners + 1) % runners.length;
//		}
//
//		trackedPlayerName = runners[indexRunners].getName().getString();
//		trackingRunner = true;
//	}
//
//	public void cycleHunters() {
//		if (hunters == null || hunters.length == 0)
//			return;
//
//		// When switching teams, it makes more sense to flip back to the previous hunter selected
//		if (!trackingRunner) {
//			indexHunters = (indexHunters + 1) % hunters.length;
//		}
//
//		trackedPlayerName = hunters[indexHunters].getName().getString();
//		trackingRunner = false;
//	}
//
//	// If the tracked player is not in the same dimension as the hunter, the targetPosition will be used.
//	private String trackedPlayerName;
//	private UUID trackedPlayerUUID;
//	private Vec3 targetPosition;
//
//	private ServerPlayer[] runners;
//	private ServerPlayer[] hunters;
//	private int indexRunners = 0;
//	private int indexHunters = 0;
//	private boolean trackingRunner = true;
//
//	private static final ArrayList<HunterCompassItem> allCompasses = new ArrayList<>();
//}
