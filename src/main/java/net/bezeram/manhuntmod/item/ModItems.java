package net.bezeram.manhuntmod.item;

import net.bezeram.manhuntmod.ManhuntMod;
import net.bezeram.manhuntmod.game_manager.Game;
import net.bezeram.manhuntmod.item.custom.HunterCompassItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, ManhuntMod.MOD_ID);

	public static final RegistryObject<Item> HUNTER_COMPASS = ITEMS.register("hunter_compass",
			() -> new HunterCompassItem(new Item.Properties().stacksTo(1)));

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
