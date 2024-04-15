package net.bezeram.manhuntmod.events;

import net.bezeram.manhuntmod.game.ClientData;
import net.bezeram.manhuntmod.gui.custom.ExtendedDeathScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "manhuntmod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void updateClient(TickEvent.ClientTickEvent event) {
        ClientData.get().update();
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (ClientData.get().isGameInSession()) {
            Screen screen = event.getScreen();
            if (screen instanceof DeathScreen deathScreen) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null)
                    event.setNewScreen(new ExtendedDeathScreen(deathScreen));
            }
        }
    }
}
