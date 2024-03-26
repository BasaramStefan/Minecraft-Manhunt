package net.bezeram.manhuntmod.game;

import java.util.Hashtable;

public class EnchantmentBanType {
	public enum DefaultBannedEnchantments {
		PIERCING("piercing", 1),
		QUICK_CHARGE("quick_charge", 1);

		DefaultBannedEnchantments(String id, int max_level) {
			this.max_level = max_level;
			this.id = id;
		}

		public final String id;
		public final int max_level;
	}

	public EnchantmentBanType() {}

	public void add(String id, int max_level) {
		if (enchantmentsMap.containsKey(id) && enchantmentsMap.get(id) > max_level) {
			return;
		}

		enchantmentsMap.put(id, max_level);
	}

	public void add(DefaultBannedEnchantments enchant) {
		if (enchantmentsMap.containsKey(enchant.id) && enchantmentsMap.get(enchant.id) > enchant.max_level) {
			return;
		}

		enchantmentsMap.put(enchant.id, enchant.max_level);
	}

	public void remove(String id) {
		enchantmentsMap.remove(id);
	}
	public void removeAll() { enchantmentsMap.clear(); }

	public void removeDefaultOptions() {
		DefaultBannedEnchantments[] allOptions = DefaultBannedEnchantments.values();
		for (DefaultBannedEnchantments option : allOptions) {
			enchantmentsMap.remove(option.id);
		}
	}

	public void addDefaultOptions() {
		DefaultBannedEnchantments[] allOptions = DefaultBannedEnchantments.values();
		for (DefaultBannedEnchantments option : allOptions) {
			enchantmentsMap.put(option.id, option.max_level);
		}
	}

	public void toggleDefaultOptions(boolean toggle) {
		enableDefaultOptions = toggle;
	}

	public boolean isEnchantBanned(String id) {
		if (enableDefaultOptions) {
			DefaultBannedEnchantments[] allOptions = DefaultBannedEnchantments.values();
			for (DefaultBannedEnchantments allOption : allOptions) {
				if (allOption.id.equals(id)) {
					return true;
				}
			}

			return false;
		}

		return enchantmentsMap.containsKey(id);
	}

	public int getEnchantMaxLevel(String id) {
		if (enchantmentsMap.containsKey(id))
			return enchantmentsMap.get(id);

		if (enableDefaultOptions) {
			DefaultBannedEnchantments[] allOptions = DefaultBannedEnchantments.values();
			for (DefaultBannedEnchantments allOption : allOptions) {
				if (allOption.id.equals(id)) {
					return allOption.max_level;
				}
			}
		}

		return -1;
	}

	private final Hashtable<String, Integer> enchantmentsMap = new Hashtable<>();
	boolean enableDefaultOptions = false;
}
