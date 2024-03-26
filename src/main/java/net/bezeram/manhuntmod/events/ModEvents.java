package net.bezeram.manhuntmod.events;

import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.commands.*;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.ManhuntGameRules;
import net.bezeram.manhuntmod.game.Time;
import net.bezeram.manhuntmod.game.players.PlayerRespawner;
import net.bezeram.manhuntmod.item.DeathSafeItems;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
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

			boolean inOverworld  = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD);
			boolean inNether 	 = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER);
			boolean inEnd 		 = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END);
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

			if (!ManhuntGameRules.canBreakSpawners() && isSpawnerBlock(event)) {
				event.setCanceled(true);
				event.getPlayer().displayClientMessage(
						Component.literal("Spawners cannot be broken!").withStyle(ChatFormatting.RED), true);
			}
		}

		private static boolean isSpawnerBlock(BlockEvent.BreakEvent event) {
			return event.getState().getBlock() == Blocks.SPAWNER;
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

			// Update scoreboard
			ServerScoreboard scoreboard = event.getServer().getScoreboard();
			Objective objective = scoreboard.getObjective("TimeLeft");

			if (objective != null) {
				int secondsLeft = (int)(Game.get().getGameTime().asSeconds() - Game.get().getElapsedTime().asSeconds());
				int minutesLeft = secondsLeft / 60;

				String scoreLabel   = (minutesLeft >= 1) ? "Minutes" : "Seconds";
				int score           = (minutesLeft >= 1) ? minutesLeft : secondsLeft;
				if (minutesLeft < 1) {
					scoreboard.resetPlayerScore("Minutes", objective);
				}

				scoreboard.getOrCreatePlayerScore(scoreLabel, objective).setScore(score);

				// Sudden death
				if (Game.get().isSuddenDeath() && !SuddenDeathWarning.hasTriggered) {
					SuddenDeathWarning.broadcastMessage(event.getServer().getPlayerList(), Game.get().getTimeLeft());
					PlayerTeam timeHighlight = scoreboard.addPlayerTeam("SuddenDeath");
					scoreboard.addPlayerToTeam("Minutes", timeHighlight);
					scoreboard.addPlayerToTeam("Seconds", timeHighlight);
				}

				if (SuddenDeathWarning.hasTriggered) {
					// Cycle highlight
					SuddenDeathWarning.updateScoreboardTime();
					PlayerTeam playerTeam = scoreboard.getPlayerTeam("SuddenDeath");

					assert playerTeam != null;
					playerTeam.setColor(SuddenDeathWarning.scoreboardTimeColor);
				}
			}
			else
				System.out.println("ERROR: Game display scoreboard has null objective");
		}

		@SubscribeEvent
		// Save player's inventory in game class
		// Destroy designated items in order to not drop them
		public static void onEntityDeath(final LivingDeathEvent event) {
			if (event.getEntity().getLevel().isClientSide() || !Game.inSession())
				return;

			if (event.getEntity() instanceof EnderDragon) {
				Game.get().runnerHasWon();
				return;
			}

			if (event.getEntity() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative())
				PlayerRespawner.playerDiedStatic(serverPlayer);
		}

        @SubscribeEvent
        public static void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
			if (event.getEntity().getLevel().isClientSide() || !Game.inSession() || event.isEndConquered())
				return;

			ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
			if (serverPlayer.isCreative())
				return;

			PlayerRespawner.playerRespawnedStatic(serverPlayer);
		}
    }
}
