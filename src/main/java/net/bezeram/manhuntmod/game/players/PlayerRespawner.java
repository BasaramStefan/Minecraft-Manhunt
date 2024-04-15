package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.game.ManhuntGameRules;
import net.bezeram.manhuntmod.game.GameTimer;
import net.bezeram.manhuntmod.item.DeathSafeItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Hashtable;

public class PlayerRespawner {
    private final Hashtable<String, Inventory> playerInventories = new Hashtable<>();
    private final GameTimer timer;

    public PlayerRespawner(final GameTimer timer) { this.timer = timer; }

    public static void playerRespawnedStatic(final ServerPlayer serverPlayer) {
        Game.get().getPlayerData().getPlayerRespawner().playerRespawned(serverPlayer);
    }

    public static void playerDiedStatic(final ServerPlayer serverPlayer) {
        Game.get().getPlayerData().getPlayerRespawner().playerDied(serverPlayer);
    }

    public void playerRespawned(final ServerPlayer player) {
        if (ManhuntGameRules.keepPartialInventory()) {
            String playerName = player.getDisplayName().getString();

            if (!isInventorySaved(playerName)) {
                player.displayClientMessage(Component
                        .literal("ERROR: Inventory has not been saved yet for player: " + playerName)
                        .withStyle(ChatFormatting.RED), false);
                return;
            }

            player.getInventory().replaceWith(getInventory(playerName));
        }

        // Reset the player's respawn position in case of portal respawn
        GlobalPos respawnPos = Game.get().getPlayerData().getRespawnBuffer(player.getUUID());
        boolean triggeredPortalRespawn = Game.get().getPlayerData().hasUsedPortalRespawn();
        if (triggeredPortalRespawn) {
            Game.LOG("Portal Respawn triggered");
            if (respawnPos != null) {
                player.setRespawnPosition(respawnPos.dimension(), respawnPos.pos(), player.getRespawnAngle(), false,
                        false);
                Game.LOG("Player position changed to the original");
            }
            Game.get().getPlayerData().setUsedPortalRespawn(false);
        }
    }

    public void playerDied(final ServerPlayer serverPlayer) {
        try {
            savePlayerInventory(serverPlayer);

            // Deduct from the game time if the serverPlayer is a runner
            if (ManhuntGameRules.isTimeLimit() && Game.get().getPlayerData().isRunner(serverPlayer)) {
                applyDeathPenalty(serverPlayer.getLevel());
            }
        } catch (Exception ignored) {}
    }

    private void applyDeathPenalty(Level level) {
        switch (ManhuntGameRules.getDeathPenalty()) {
            case TRUE -> timer.applyDeathPenalty();
            case TRUE_EXCEPT_END -> {
                if (level.dimension() == Level.END)
                    timer.applyDeathPenalty();
            }
        }
    }

    private void savePlayerInventory(final ServerPlayer serverPlayer) {
        final int SLOT_COUNT = 41;

        try {
            if (ManhuntGameRules.keepPartialInventory()) {
                // Directly save inventory
                if (ManhuntGameRules.keepInventoryEnd() && serverPlayer.getLevel().dimension() == Level.END) {
                    Inventory savedInventory = new Inventory(serverPlayer);
                    savedInventory.replaceWith(serverPlayer.getInventory());
                    serverPlayer.getInventory().clearContent();
                    saveInventory(serverPlayer.getDisplayName().getString(), savedInventory);
                    return;
                }

                // Save the serverPlayer's inventory, which is loaded on the first tick after respawn
                Inventory savedInventory = new Inventory(serverPlayer);
                for (int slot = 0; slot < SLOT_COUNT; slot++) {
                    ItemStack itemStack = serverPlayer.getInventory().getItem(slot);

                    if (DeathSafeItems.isDeathSafe(itemStack.getItem())) {
                        savedInventory.setItem(slot, itemStack);
                        serverPlayer.getInventory().setItem(slot, ItemStack.EMPTY);
                    }

                    if (DeathSafeItems.isException(itemStack.getItem())) {
                        Item converted = DeathSafeItems.convertExceptionItem(itemStack.getItem());
                        savedInventory.setItem(slot, new ItemStack(converted));
                        serverPlayer.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                }

                saveInventory(serverPlayer.getDisplayName().getString(), savedInventory);
            }
        } catch (Exception ignored) {}
    }

    public static void saveInventoryStatic(final String playerDisplayName, final Inventory inventory) {
        Game.get().getPlayerData().getPlayerRespawner().saveInventory(playerDisplayName, inventory);
    }

    public static Inventory getInventoryStatic(final String playerDisplayName) {
        return Game.get().getPlayerData().getPlayerRespawner().getInventory(playerDisplayName);
    }

    private void saveInventory(final String playerDisplayName, final Inventory inventory) {
        playerInventories.put(playerDisplayName, inventory);
    }

    public final Inventory getInventory(final String playerDisplayName) {
        if (!playerInventories.containsKey(playerDisplayName))
            return null;
        return playerInventories.get(playerDisplayName);
    }

    public boolean isInventorySaved(final String playerDisplayName) {
        return playerInventories.containsKey(playerDisplayName);
    }
}
