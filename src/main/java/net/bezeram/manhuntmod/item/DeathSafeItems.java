package net.bezeram.manhuntmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Hashtable;

public class DeathSafeItems {
	private final static Hashtable<Item, Integer> items = new Hashtable<>();
	private final static Hashtable<Item, Item> exceptions = new Hashtable<>();

	public static void registerItems()
	{
			// Wooden tools
			items.put(Items.WOODEN_AXE, 0);
			items.put(Items.WOODEN_HOE, 0);
			items.put(Items.WOODEN_PICKAXE, 0);
			items.put(Items.WOODEN_SHOVEL, 0);
			items.put(Items.WOODEN_SWORD, 0);

			// Stone tools
			items.put(Items.STONE_AXE, 0);
			items.put(Items.STONE_HOE, 0);
			items.put(Items.STONE_PICKAXE, 0);
			items.put(Items.STONE_SHOVEL, 0);
			items.put(Items.STONE_SWORD, 0);

			// Golden tools
			items.put(Items.GOLDEN_AXE, 0);
			items.put(Items.GOLDEN_HOE, 0);
			items.put(Items.GOLDEN_PICKAXE, 0);
			items.put(Items.GOLDEN_SHOVEL, 0);
			items.put(Items.GOLDEN_SWORD, 0);

			// Iron tools
			items.put(Items.IRON_AXE, 0);
			items.put(Items.IRON_HOE, 0);
			items.put(Items.IRON_PICKAXE, 0);
			items.put(Items.IRON_SHOVEL, 0);
			items.put(Items.IRON_SWORD, 0);

			// Diamond tools
			items.put(Items.DIAMOND_AXE, 0);
			items.put(Items.DIAMOND_HOE, 0);
			items.put(Items.DIAMOND_PICKAXE, 0);
			items.put(Items.DIAMOND_SHOVEL, 0);
			items.put(Items.DIAMOND_SWORD, 0);

			// Netherite tools
			items.put(Items.NETHERITE_AXE, 0);
			items.put(Items.NETHERITE_HOE, 0);
			items.put(Items.NETHERITE_PICKAXE, 0);
			items.put(Items.NETHERITE_SHOVEL, 0);
			items.put(Items.NETHERITE_SWORD, 0);

			// Leather armour
			items.put(Items.LEATHER_HELMET, 0);
			items.put(Items.LEATHER_CHESTPLATE, 0);
			items.put(Items.LEATHER_LEGGINGS, 0);
			items.put(Items.LEATHER_BOOTS, 0);

			// Chainmail armour
			items.put(Items.CHAINMAIL_HELMET, 0);
			items.put(Items.CHAINMAIL_CHESTPLATE, 0);
			items.put(Items.CHAINMAIL_LEGGINGS, 0);
			items.put(Items.CHAINMAIL_BOOTS, 0);

			// Golden armour
			items.put(Items.GOLDEN_HELMET, 0);
			items.put(Items.GOLDEN_CHESTPLATE, 0);
			items.put(Items.GOLDEN_LEGGINGS, 0);
			items.put(Items.GOLDEN_BOOTS, 0);

			// Iron armour
			items.put(Items.IRON_HELMET, 0);
			items.put(Items.IRON_CHESTPLATE, 0);
			items.put(Items.IRON_LEGGINGS, 0);
			items.put(Items.IRON_BOOTS, 0);

			// Diamond armour
			items.put(Items.DIAMOND_HELMET, 0);
			items.put(Items.DIAMOND_CHESTPLATE, 0);
			items.put(Items.DIAMOND_LEGGINGS, 0);
			items.put(Items.DIAMOND_BOOTS, 0);

			// Netherite armour
			items.put(Items.NETHERITE_HELMET, 0);
			items.put(Items.NETHERITE_CHESTPLATE, 0);
			items.put(Items.NETHERITE_LEGGINGS, 0);
			items.put(Items.NETHERITE_BOOTS, 0);

			// Boats
			items.put(Items.OAK_BOAT, 0);
			items.put(Items.BIRCH_BOAT, 0);
			items.put(Items.SPRUCE_BOAT, 0);
			items.put(Items.ACACIA_BOAT, 0);
			items.put(Items.DARK_OAK_BOAT, 0);
			items.put(Items.JUNGLE_BOAT, 0);
			items.put(Items.CHERRY_BOAT, 0);
			items.put(Items.MANGROVE_BOAT, 0);

			// Chest boats
			items.put(Items.OAK_CHEST_BOAT, 0);
			items.put(Items.BIRCH_CHEST_BOAT, 0);
			items.put(Items.SPRUCE_CHEST_BOAT, 0);
			items.put(Items.ACACIA_CHEST_BOAT, 0);
			items.put(Items.DARK_OAK_CHEST_BOAT, 0);
			items.put(Items.JUNGLE_CHEST_BOAT, 0);
			items.put(Items.CHERRY_CHEST_BOAT, 0);
			items.put(Items.MANGROVE_CHEST_BOAT, 0);

			// General tools
			items.put(Items.SHIELD, 0);
			items.put(Items.FISHING_ROD, 0);
			items.put(Items.FLINT_AND_STEEL, 0);
			items.put(Items.BUCKET, 0);
			items.put(Items.WATER_BUCKET, 0);
			items.put(Items.POWDER_SNOW_BUCKET, 0);
			items.put(Items.BOW, 0);
			items.put(Items.CROSSBOW, 0);
			items.put(Items.LEAD, 0);
			items.put(Items.TRIDENT, 0);
			items.put(Items.ARROW, 0);
			items.put(Items.SPECTRAL_ARROW, 0);
			items.put(Items.POTION, 0);
			items.put(Items.ANVIL, 0);
			items.put(Items.CHIPPED_ANVIL, 0);
			items.put(Items.DAMAGED_ANVIL, 0);
			items.put(Items.ENCHANTING_TABLE, 0);
			items.put(Items.GOLDEN_APPLE, 0);
			items.put(Items.SLIME_BLOCK, 0);

			// Beds and anchor
			items.put(Items.RESPAWN_ANCHOR, 0);
			items.put(Items.GLOWSTONE_DUST, 0);
			items.put(Items.GLOWSTONE, 0);
			items.put(Items.WHITE_BED, 0);
			items.put(Items.LIGHT_GRAY_BED, 0);
			items.put(Items.GRAY_BED, 0);
			items.put(Items.BLACK_BED, 0);
			items.put(Items.BROWN_BED, 0);
			items.put(Items.RED_BED, 0);
			items.put(Items.ORANGE_BED, 0);
			items.put(Items.YELLOW_BED, 0);
			items.put(Items.LIME_BED, 0);
			items.put(Items.GREEN_BED, 0);
			items.put(Items.CYAN_BED, 0);
			items.put(Items.LIGHT_BLUE_BED, 0);
			items.put(Items.BLUE_BED, 0);
			items.put(Items.PURPLE_BED, 0);
			items.put(Items.PINK_BED, 0);
			items.put(Items.MAGENTA_BED, 0);
	};

	public static void registerExceptions() {
		exceptions.put(Items.LAVA_BUCKET, Items.BUCKET);
	}

	public static boolean isDeathSafe(Item item)    { return items.containsKey(item); }
	public static boolean isException(Item item)    { return exceptions.containsKey(item); }
	public static Item convertExceptionItem(Item item)  { return exceptions.get(item); }
}
