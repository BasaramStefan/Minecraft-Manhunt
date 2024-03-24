package net.bezeram.manhuntmod;

import net.bezeram.manhuntmod.game.ClientData;
import net.bezeram.manhuntmod.item.ModItems;
import net.bezeram.manhuntmod.networking.ModMessages;
import net.bezeram.manhuntmod.networking.packets.HunterCompassGetPosC2SPacket;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static net.bezeram.manhuntmod.item.custom.HunterCompassItem.TAG_TARGET_PLAYER;
import static net.bezeram.manhuntmod.item.custom.HunterCompassItem.TAG_TARGET_TRACKING;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ManhuntMod.MOD_ID)
public class ManhuntMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "manhuntmod";

    public ManhuntMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);

        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);


        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModMessages.register();
    }

    private void addCreative(CreativeModeTabEvent.BuildContents event) {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(ModItems.HUNTER_COMPASS.get(),
                    new ResourceLocation(ManhuntMod.MOD_ID, "angle"),
                    new CompassItemPropertyFunction((clientLevel, itemStack, entity) -> {
                        int MAID = itemStack.getOrCreateTag().getInt(TAG_TARGET_PLAYER);
                        boolean isTracking = itemStack.getOrCreateTag().getBoolean(TAG_TARGET_TRACKING);
                        ModMessages.sendToServer(new HunterCompassGetPosC2SPacket(MAID, isTracking));

                        return GlobalPos.of(clientLevel.dimension(), new BlockPos(
                                ClientData.get().getCompassData().targetX,
                                0,
                                ClientData.get().getCompassData().targetZ)
                        );
                    }
            )));
        }
    }
}
