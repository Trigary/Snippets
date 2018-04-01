private <T> T loadJson(String fileName, Type type) {
	File file = new File(getDataFolder(), fileName);
	if (!file.exists() || file.length() <= 0) {
		return null;
	}
	
	try (FileReader reader = new FileReader(file)) {
		return GSON.fromJson(reader, type);
	} catch (IOException e) {
		e.printStackTrace();
		return null;
	}
}

private void saveJson(String fileName, Object serializable) {
	File file = new File(getDataFolder(), fileName);
	file.getParentFile().mkdirs();
	try (FileWriter writer = new FileWriter(file)) {
		writer.write(GSON.toJson(serializable, serializable.getClass()));
	} catch (IOException e) {
		e.printStackTrace();
	}
}
