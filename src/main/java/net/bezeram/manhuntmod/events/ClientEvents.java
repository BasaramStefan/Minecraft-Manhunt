package net.bezeram.manhuntmod.events;

import net.bezeram.manhuntmod.game.ClientData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "manhuntmod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void updateClient(TickEvent.ClientTickEvent event) {
        ClientData.get().update();
    }
}
