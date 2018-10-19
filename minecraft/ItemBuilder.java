import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ItemBuilder {
	private static final Map<Class, Field> PROFILE_FIELDS = new IdentityHashMap<>();
	
	private final ItemStack item;
	private final ItemMeta meta;
	
	public ItemBuilder(Material material) {
		item = new ItemStack(material);
		meta = item.getItemMeta();
	}
	
	
	
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
	
	public ItemBuilder setAmount(int amount) {
		item.setAmount(amount);
		return this;
	}
	
	public ItemBuilder setDurability(int durability) {
		item.setDurability((short)durability);
		return this;
	}
	
	
	
	public ItemBuilder setName(String name) {
		meta.setDisplayName(name);
		return this;
	}
	
	public ItemBuilder setLore(List<String> lore) {
		meta.setLore(lore);
		return this;
	}
	
	public ItemBuilder setLore(String... lore) {
		meta.setLore(Arrays.asList(lore));
		return this;
	}
	
	
	
	public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
		meta.addEnchant(enchantment, level, true);
		return this;
	}
	
	public ItemBuilder addItemFlags(ItemFlag... flags) {
		meta.addItemFlags(flags);
		return this;
	}
	
	public ItemBuilder setUnbreakable(boolean unbreakable) {
		meta.setUnbreakable(unbreakable);
		return this;
	}
	
	
	
	public <T extends ItemMeta> ItemBuilder applyCustomMeta(Class<T> type, Consumer<T> applier) {
		applier.accept(type.cast(meta));
		return this;
	}
	
	
	
	public ItemBuilder setPlayerSkull(String playerName) {
		SkullMeta skullMeta = (SkullMeta)meta;
		skullMeta.setOwner(playerName);
		return this;
	}
	
	public ItemBuilder setCustomSkull(String url) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", "http://textures.minecraft.net/texture/" + url).getBytes());
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));

		try {
			Field profileField = PROFILE_FIELDS.computeIfAbsent(meta.getClass(), metaClass -> {
				try {
					Field profileField = metaClass.getDeclaredField("profile");
					profileField.setAccessible(true);
					return profileField;
				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
					return null;
				}
			});

			if (profileField != null) profileField.set(meta, profile);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}

		return this;
	}
}
