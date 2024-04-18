package net.bezeram.manhuntmod.events;

import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.commands.*;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.ManhuntGameRules;
import net.bezeram.manhuntmod.game.Time;
import net.bezeram.manhuntmod.game.players.EndLockLogic;
import net.bezeram.manhuntmod.game.players.PlayerRespawner;
import net.bezeram.manhuntmod.item.DeathSafeItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Mod.EventBusSubscriber(modid = ManhuntMod.MOD_ID)
public class ModEvents {

	@SubscribeEvent
	public static void onCommandsRegister(RegisterCommandsEvent event) {
		new ManhuntCommand(event.getDispatcher());
		new ManhuntTimerCommand(event.getDispatcher());
		new ManhuntRulesCommand(event.getDispatcher());
		new DebugCommand(event.getDispatcher());

		DeathSafeItems.registerItems();
		DeathSafeItems.registerExceptions();

		ManhuntGameRules.BAN_ENCHANTMENTS.toggleDefaultOptions(true);

		ConfigCommand.register(event.getDispatcher());
	}

	@Mod.EventBusSubscriber(modid = ManhuntMod.MOD_ID)
	public static class ForgeEvents {
		public static class SuddenDeathWarning {
			public static final Time HIGHLIGHT_CYCLE_DELAY = Time.TimeSeconds(30);
			public static final Time HIGHLIGHT_CHANGE_DELAY_TIME = Time.TimeSeconds(0.25f);

			public static void broadcastMessage(PlayerList playerList, Time timeLeft) {
				playerList.broadcastSystemMessage(Component
						.literal("Sudden death! Runner loses on next death")
						.withStyle(ChatFormatting.DARK_RED), false);
				hasTriggered = true;
			}

			public static void updateScoreboardTime() {
				highlightCycleTimer.advance();
				if (highlightCycleTimer.asTicks() >= HIGHLIGHT_CYCLE_DELAY.asTicks()) {
					highlightChangeDelayTimer.advance();
					if (highlightChangeDelayTimer.asTicks() >= HIGHLIGHT_CHANGE_DELAY_TIME.asTicks()) {
						switch (scoreboardTimeColor) {
							case RED -> scoreboardTimeColor = ChatFormatting.WHITE;
							case WHITE -> scoreboardTimeColor = ChatFormatting.RED;
						}

						counter++;
						if (counter == 4) {
							highlightCycleTimer.setTicks(0);
							counter = 0;
						}

						highlightChangeDelayTimer.setTicks(0);
					}
				}
			}

			public static boolean hasTriggered = false;
			public static ChatFormatting scoreboardTimeColor = ChatFormatting.RED;
			private static int counter = 0;
			public static Time highlightCycleTimer = Time.TimeTicks(0);
			public static Time highlightChangeDelayTimer = Time.TimeTicks(0);
		}

		@SubscribeEvent
		public static void disableExplosives(PlayerInteractEvent.RightClickBlock event) {
			if (event.getSide().isClient()
					|| !Game.inSession()
					|| event.getEntity().isCreative()
					|| !ManhuntGameRules.DISABLE_RESPAWN_BLOCK_EXPLOSION)
				return;

			Item mainHandItem = event.getEntity().getMainHandItem().getItem();
			Item offHandItem = event.getEntity().getOffhandItem().getItem();

			if (ManhuntGameRules.BAN_END_CRYSTALS && (mainHandItem == Items.END_CRYSTAL || offHandItem == Items.END_CRYSTAL)) {
				event.setUseItem(Event.Result.DENY);
				event.getEntity().displayClientMessage(Component
						.literal("End Crystals cannot be placed!").withStyle(ChatFormatting.RED), true);
			}

			BlockPos blockPos = event.getPos();
			Level level = event.getLevel();
			BlockState blockState = level.getBlockState(blockPos);

			boolean inOverworld  = level.dimension() == Level.OVERWORLD;
			boolean inNether 	 = level.dimension() == Level.NETHER;
			boolean inEnd 		 = level.dimension() == Level.END;
			boolean isBed		 = blockState.isBed(level, blockPos, null);
			boolean isAnchor	 = blockState.getBlock() == Blocks.RESPAWN_ANCHOR;

			if (((inNether || inEnd) && isBed) || ((inOverworld || inEnd) && isAnchor)) {
				event.setUseBlock(Event.Result.DENY);
			}
		}

		@SubscribeEvent
		public static void disableBreakingBlocks(BlockEvent.BreakEvent event) {
			if (event.getLevel().isClientSide() || !Game.inSession() || event.getPlayer().isCreative())
				return;

			if (Game.isHunterAtGameState(event.getPlayer(), Game.GameState.START) ||
				Game.isHunterAtGameState(event.getPlayer(), Game.GameState.HEADSTART) ||
				Game.isRunnerAtGameState(event.getPlayer(), Game.GameState.START) ||
				Game.getGameState() == Game.GameState.PAUSE ||
				Game.getGameState() == Game.GameState.RESUME)
			{
				event.setCanceled(true);
				return;
			}

			isSilverfishSpawner(event.getState());
			if (!ManhuntGameRules.canBreakSpawners() && isSpawnerBlock(event)) {
				event.setCanceled(true);
				event.getPlayer().displayClientMessage(
						Component.literal("Spawners cannot be broken!").withStyle(ChatFormatting.RED), true);
			}
		}

		private static boolean isSpawnerBlock(BlockEvent.BreakEvent event) {
			return event.getState().getBlock() == Blocks.SPAWNER;
		}

		private static boolean isSilverfishSpawner(final BlockState blockState) {
			for (var property : blockState.getProperties()) {
				Game.LOG(property.toString());
			}

			return false;
		}

		@SubscribeEvent
		public static void onServerTick(TickEvent.ServerTickEvent event) {
			if (!Game.inSession())
				return;

			Game.get().update(event);
			if (Game.getGameState() == Game.GameState.ERASE) {
				Game.get().stopGame();
				return;
			}
			Game.get().updateScoreboard(event);

			Game.get().ifPlayersInPortal();
		}

		@SubscribeEvent
		// Execute custom player respawner
		public static void onEntityDeath(final LivingDeathEvent event) {
			if (!Game.inSession() || event.getEntity().getLevel().isClientSide)
				return;

			if (event.getEntity() instanceof EnderDragon) {
				Game.get().runnerHasWon();
				return;
			}

			try {
				if (event.getEntity() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
					PlayerRespawner.playerDiedStatic(serverPlayer);
				}
			} catch (Exception ignored) {}
		}

        @SubscribeEvent
        public static void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
			if (event.getEntity().getLevel().isClientSide() || !Game.inSession() || event.isEndConquered())
				return;

			try {
				ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
				if (serverPlayer.isCreative())
					return;

				PlayerRespawner.playerRespawnedStatic(serverPlayer);

			} catch(Exception ignored) {}
		}

		@SubscribeEvent
		public static void onPlayerChangeDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!Game.inSession() || event.getEntity().getLevel().isClientSide)
				return;

			if (event.getTo() == Level.END) {
				// End Lock
				// The player is now respawn locked to the stronghold they entered.
				// The spawn is automatically set and subsequently cannot be changed.
				ServerPlayer player = (ServerPlayer)event.getEntity();
				boolean isEndLocked = Game.get().getPlayerData().isEndLocked(player.getUUID());
				if (!isEndLocked) {
					Game.LOG("Setting player to End Locked: " + player.getName().getString());
					EndLockLogic.calculateRespawnAndLock(player);
				}
				else
					Game.LOG("Player: " + player.getName().getString() + " is already End Locked");
			}
		}
    }
}
