package net.bezeram.manhuntmod.events;

import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.commands.*;
import net.bezeram.manhuntmod.game_manager.Game;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = ManhuntMod.MOD_ID)
public class ModEvents {

	@SubscribeEvent
	public static void onCommandsRegister(RegisterCommandsEvent event) {
		new ManhuntSetSpawnCountCommand(event.getDispatcher());
		new ManhuntCommand(event.getDispatcher());
		new ManhuntToggleRulesCommand(event.getDispatcher());
		new ManhuntTimerCommand(event.getDispatcher());
		new DebugCommand(event.getDispatcher());

		ConfigCommand.register(event.getDispatcher());
	}

	@Mod.EventBusSubscriber(modid = ManhuntMod.MOD_ID)
	public static class ForgeEvents {

		@SubscribeEvent
		public static void onServerTick(TickEvent.ServerTickEvent event) {
			if (Game.isInSession()) {
				// Game is on
				if (Game.getGameState() == Game.GameState.GAME_END) {
					Game.stopGame();
					return;
				}

				Game.get().update(event);
			}
		}

		@SubscribeEvent
		public static void disableIntentionalGameDesign(PlayerInteractEvent.RightClickBlock event) {
			if (Game.rulesEnabled()) {
				BlockPos blockPos = event.getPos();
				Level level = event.getLevel();

				boolean inOverworld  = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD);
				boolean inNether 	 = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER);
				boolean inEnd 		 = level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END);
				boolean isBed		 = level.getBlockState(blockPos).isBed(level, blockPos, null);
				boolean isAnchor	 = level.getBlockState(blockPos).getBlock() instanceof RespawnAnchorBlock;

				if (((inNether || inEnd) && isBed) || ((inOverworld || inEnd) && isAnchor)) {
					event.setUseBlock(Event.Result.DENY);
				}
			}
		}
	}
}
