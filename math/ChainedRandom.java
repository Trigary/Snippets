//simulates multiple "dice rolls" where the favourable number has a chance of 'chance' to occur
//returns the count of times where this favourable number was rolled in a row (we stop as soon as we get a different number)
// return value of X has a chance of 'chance'^X
// (eg. return value of 2 has a chance of 'chance' * 'chance')
public static int chainedRandomAmount(double chance, int maxAmount) {
	if (chance < 0 || chance >= 1 || maxAmount < 0) {
		throw new IllegalArgumentException();
	}
	long result = (long) Math.floor(Math.log(ThreadLocalRandom.current().nextDouble()) / Math.log(chance));
	return result < maxAmount ? (int) result : maxAmount;
}
