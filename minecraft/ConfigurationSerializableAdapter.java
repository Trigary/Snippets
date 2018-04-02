public class ConfigurationSerializableAdapter implements JsonSerializer<ConfigurationSerializable>, JsonDeserializer<ConfigurationSerializable> {
	private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
	
	@Override
	public ConfigurationSerializable deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		return (ConfigurationSerializable) deserialize((Map<String, Object>) context.deserialize(json, MAP_TYPE));
	}
	
	private Object deserialize(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			entry.setValue(deserialize(entry.getValue()));
		}
		
		try {
			return map.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)
					? ConfigurationSerialization.deserializeObject(map)
					: map;
		} catch (Throwable t) {
			throw new JsonParseException(t);
		}
	}
	
	private void deserialize(List<Object> list) {
		for (int i = 0; i < list.size(); i++) {
			list.set(i, deserialize(list.get(i)));
		}
	}
	
	private Object deserialize(Object object) {
		if (object instanceof Map) {
			return deserialize((Map<String, Object>) object);
		} else if (object instanceof List) {
			deserialize((List<Object>) object);
			return object;
		} else if (object instanceof Number) {
			Number value = (Number) object;
			if (value.doubleValue() == vale.intValue()) {
				return value.intValue();
			}
		}
		return object;
	}
	
	@Override
	public JsonElement serialize(ConfigurationSerializable object, Type type, JsonSerializationContext context) {
		Map<String, Object> map = new HashMap<>(object.serialize());
		map.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(object.getClass()));
		return context.serialize(map, MAP_TYPE);
	}
}
