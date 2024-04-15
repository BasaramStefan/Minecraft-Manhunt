package net.bezeram.manhuntmod.gui.custom;

import net.bezeram.manhuntmod.game.ClientData;
import net.bezeram.manhuntmod.game.Game;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.PortalRespawnerC2SPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExtendedDeathScreen extends DeathScreen {

    private int delayTicker;
    private Button altRespawnButton = null;
    private boolean isAltOpen;

    public ExtendedDeathScreen(final DeathScreen deathScreen) {
        super(deathScreen.causeOfDeath, deathScreen.hardcore);
    }

    @Override
    protected void init() {
        super.init();

        this.delayTicker = 0;
        Component component = Component.translatable("deathScreen.manhuntmod.portalRespawn");

        // TODO: add alternative respawn widget IF the player has entered the nether at least once
        //  send a packet to server to see if the button should be added
        BlockPos portalCoords = ClientData.get().getPortalRespawnCoords();
        isAltOpen = portalCoords != null;
        if (ClientData.get().isGameInSession()) {
            altRespawnButton = buildAltRespawnButton(component,
                    (button) -> {
                        // TODO: Custom Respawner
                        //  send packet to server to change respawn point
                        ModMessages.sendToServer(new PortalRespawnerC2SPacket());
                        button.active = false;
                    }
            );
            this.addRenderableWidget(altRespawnButton);
            this.setPortalRespawnButtonActive(false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ++this.delayTicker;
        if (this.delayTicker == 20) {
            if (isAltOpen) {
                this.setPortalRespawnButtonActive(true);
            }
        }
        if (respawnPointChangeAcknowledged()) {
            if (minecraft == null || minecraft.player == null) {
                Game.LOG("Could not respawn player due to minecraft or player instance being null");
                return;
            }

            minecraft.player.respawn();
            Game.LOG("Portal Respawn executed");
            ClientData.get().respawnPointAcknowledgeReset();
        }
    }

    private void setPortalRespawnButtonActive(boolean active) {
        if (altRespawnButton != null)
            altRespawnButton.active = active;
    }

    private boolean respawnPointChangeAcknowledged() {
        return ClientData.get().isRespawnPointChangeAcknowledged();
    }

    private Button buildAltRespawnButton(Component component, Button.OnPress adder) {
        return Button.builder(component, adder)
                    .bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build();
    }
}
