package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.enums.DimensionID;
import net.bezeram.manhuntmod.game.Game;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;

public class EndLockLogic {

    /**
     * Calculate respawn position for Manhunt Player once they've entered the End.<br>
     * At the moment, runners and hunters both get respawned at the end portal.<br>
     * This remains until I figure out how to properly balance the constraints or how to teleport directly in the
     * Stronghold.
     * @param player Player who entered the End.
     */
    public static void calculateRespawnAndLock(final ServerPlayer player) {
        try {
            if (!Game.inSession() || player.getLevel().isClientSide)
                return;

            BlockPos respawnPos = getPortalRespawnPosition(player);
            PlayerData playerData = Game.get().getPlayerData();
            playerData.saveEndRespawnPosition(player.getUUID(), respawnPos);
        } catch (Exception ignored) {}
    }

    private static BlockPos getPortalRespawnPosition(final ServerPlayer player) {
        // Get the respawn position directly at the end portal
        BlockPos framePos = getPortalFrameBlock(player, 6);
        if (framePos != null) {
            Game.LOG("Found end portal frame at " + framePos);
            return framePos.above();
        }

        Game.LOG("Could not find end portal frame near player with inflated bounding box by a value of 6");
        return getStrongholdEntrancePosition(player);
    }

    public static BlockPos getPortalFrameBlock(final ServerPlayer player, int size) {
        try {
            // Check for end portal frames around a specified bounding box
            // During Manhunt, the player's last overworld location must be used, since the player
            // by then has entered the End.
            // Otherwise, the function simply uses the player as reference.

            if (player == null) {
                Game.LOG("[GetEndPortalFrame] Provided player is null: ");
                return null;
            }

            Vec3 lastPosition;
            if (Game.inSession()) {
                lastPosition = Game.get().getPlayerData().getLastPosition(DimensionID.OVERWORLD, player);
                if (lastPosition == null) {
                    Game.LOG("[GetEndPortalFrame] Impossible. Last player position in Overworld is null for player: " + player.getName().getString());
                    return null;
                }
            }
            else
                lastPosition = player.getPosition(1);

            BlockPos lastBlockPosition = BlockPos.containing(lastPosition);
            AABB boundingBox = new AABB(lastBlockPosition).inflate(size);
            Iterator<BlockPos> iter = BlockPos.betweenClosed(
                    (int)boundingBox.minX, (int)boundingBox.minY, (int)boundingBox.minZ,
                    (int)boundingBox.maxX, (int)boundingBox.maxY, (int)boundingBox.maxZ).iterator();

            Game.LOG("Searching for end portal frame for player: " + player.getName().getString());
            Game.LOG("Searching from position: " + lastBlockPosition);

            ServerLevel level;
            if (Game.inSession()) {
                level = Game.get().getServer().getLevel(ServerLevel.OVERWORLD);
                Game.LOG("Checking for Manhunt");
            }
            else  {
                // Just get the player's level
                level = player.getLevel();
                Game.LOG("Checking for debug");
            }

            while (iter.hasNext()) {
                BlockPos blockPos = iter.next();
                Block block = level.getBlockState(blockPos).getBlock();
                if (block == Blocks.END_PORTAL_FRAME)
                    return blockPos;
            }

            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * This was meant to be used to respawn the End Locked hunters.
     * @param player If player is null, the function returns null
     * @return The (0, 0) Chunk coordinate of the starter staircase of the Stronghold.
     */
    public static BlockPos getStrongholdEntrancePosition(final ServerPlayer player) {
        try {
            if (player == null) {
                Game.LOG("[GetStrongholdEntrance] Player is null");
                return null;
            }

            BlockPos lastPositionBlock;
            if (!Game.inSession()) {
                // just assume the player is in the overworld
                lastPositionBlock = player.blockPosition();
                Game.LOG("Using the player's position");
            }
            else {
                Vec3 lastPosition = Game.get().getPlayerData().getLastPosition(DimensionID.OVERWORLD, player);
                if (lastPosition != null) {
                    lastPositionBlock = BlockPos.containing(lastPosition);
                    Game.LOG("Using the player's last position");
                }
                else {
                    Game.LOG("Could not get the last or current position for the player: " + player.getName().getString());
                    return null;
                }
            }

            Game.LOG("Finding stronghold respawn position for player: " + player.getName().getString() + " from " + lastPositionBlock);
            // When this is run, the player is supposedly in the End, so we cannot use the player's current position
            ServerLevel level;
            if (Game.inSession()) {
                level = Game.get().getServer().getLevel(ServerLevel.OVERWORLD);
                Game.LOG("Checking for Manhunt");
            }
            else  {
                // Just get the player's level
                level = player.getLevel();
                Game.LOG("Checking for debug");
            }

            BlockPos blockPos = level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, lastPositionBlock,
                    100, false);

            if (blockPos != null)
                Game.LOG("Found stronghold at: " + blockPos);
            else
                Game.LOG("Did not find stronghold");
            return blockPos;
        } catch (Exception ignored) {}

        return null;
    }
}
