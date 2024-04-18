package net.bezeram.manhuntmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Hashtable;

/**
 * Items are stored in 2 categories. Normal items, with an associated integer count, and exceptions.<br>
 * For every item, the count describes if any amount should be dropped by a player if they are killed by another
 * player.<br>
 * Exceptions are items that get replaced by another item when dropped. For example, a lava bucket will drop a bucket.
 */
public class DeathSafeItems {
	private final static int ARROW_DROP_COUNT = 32;
	private final static Hashtable<Item, Integer> items = new Hashtable<>();
	private final static Hashtable<Item, Item> exceptions = new Hashtable<>();

	public static void registerDefault() {
		Item[] safe = new Item[]{
			// Wooden tools
			Items.WOODEN_AXE, Items.WOODEN_HOE, Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD,
			// Stone tools
			Items.STONE_AXE, Items.STONE_HOE, Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_SWORD,
			// Golden tools
			Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_SWORD,
			// Iron tools
			Items.IRON_AXE, Items.IRON_HOE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_SWORD,
			// Diamond tools
			Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD,
			// Netherite tools
			Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_SWORD,
			// Leather armour
			Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
			// Chainmail armour
			Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
			// Golden armour
			Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
			// Iron armour
			Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
			// Diamond armour
			Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
			// Netherite armour
			Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
			// Misc tools
			ModItems.HUNTER_COMPASS.get(), Items.SHIELD, Items.BUCKET, Items.WATER_BUCKET, Items.POWDER_SNOW_BUCKET,
			Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.POTION, Items.ARROW, Items.SPECTRAL_ARROW
		};

		for (Item item : safe) {
			items.put(item, 0);
		}

		registerExceptions();
	}

	public static void registerExceptions() {
		exceptions.put(Items.LAVA_BUCKET, Items.BUCKET);
	}

	public static boolean isDeathSafe(Item item)    { return items.containsKey(item); }
	public static boolean isException(Item item)    { return exceptions.containsKey(item); }
	public static Item convertExceptionItem(Item item)  { return exceptions.get(item); }
}
