package net.bezeram.manhuntmod.game.players;

import net.bezeram.manhuntmod.game.Game;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Hashtable;
import java.util.UUID;

public class PlayerCoords {

    public void update(final UUID uuid, final Vec3 newPosition) {
        coords.put(uuid, newPosition);
    }

    public void update(final UUID uuid) {
        if (!Game.inSession()) {
            System.out.println("PlayerCoords::update() - Game not in session\n");
            return;
        }

        ServerPlayer player = Game.get().getPlayer(uuid);
        PlayerCoords coords = playerData.getCoords(player.getLevel().dimension());

        if (coords != null)
            coords.update(uuid, player.getPosition(1));
    }

    public Vec3 get(final UUID uuid) {
        if (!coords.containsKey(uuid)) {
            coords.put(uuid, new Vec3(0, 0 ,0));
            update(uuid);
        }

        return coords.get(uuid);
    }

    public PlayerCoords(final PlayerData playerData) { this.playerData = playerData; }

    // Links the Manhunt ID (MAID) of the player to coords
    private final Hashtable<UUID, Vec3> coords = new Hashtable<>();
    private final PlayerData playerData;
}
