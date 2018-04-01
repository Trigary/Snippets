public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
	//register as hierarchy adapter -> works for ItemStack[] as well
	
	@Override
	public ItemStack deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		try (ByteArrayInputStream stream = new ByteArrayInputStream(Base64Coder.decodeLines(json.getAsString()))) {
			return (ItemStack) new BukkitObjectInputStream(stream).readObject();
		} catch (ClassNotFoundException | IOException e) {
			throw new JsonParseException(e);
		}
	}
	
	@Override
	public JsonElement serialize(ItemStack item, Type type, JsonSerializationContext context) {
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			new BukkitObjectOutputStream(stream).writeObject(item);
			return new JsonPrimitive(Base64Coder.encodeLines(stream.toByteArray()));
		} catch (IOException e) {
			throw new JsonParseException(e);
		}
	}
}
