// [(1,2), (3,4), 5] -> [(1,3,5), (1,4,5), (2,3,5), (2,4,5)]
static <T> List<List<T>> permutations(List<List<T>> source) {
	int count = source.stream().mapToInt(List::size).reduce(1, (a, b) -> a * b);
	List<List<T>> output = new ArrayList<>(count);
	int[] indexes = new int[source.size()];
	
	for (int outer = 0; outer < count; outer++) {
		output.add(IntStream.range(0, source.size())
				.mapToObj(i -> source.get(i).get(indexes[i]))
				.collect(Collectors.toList()));
		
		for (int i = source.size() - 1; i >= 0; i--) {
			if (indexes[i] + 1 < source.get(i).size()) {
				indexes[i]++;
				break;
			}
			indexes[i] = 0;
		}
	}
	return output;
}
