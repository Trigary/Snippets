/// <summary>
/// Computes a CRC for the specified (9 bits long) value.
/// Returns the value with the CRC attached or -1, if the input is out of bounds.
/// </summary>
public static int AddCrc(int value) {
	return value < 0 || value >= 512 ? -1 : value | (CalculateCrc(value) << 9);
}

/// <summary>
/// Removes the CRC from the (9 bits long) input.
/// Returns the original value or -1, if the input is out of bounds or if the CRC caught an error.
/// </summary>
public static int RemoveCrc(int value) {
	if (value < 0 || value >= 4096) {
		return -1;
	}

	int payload = value & 0b000111111111;
	return (value >> 9) != CalculateCrc(payload) ? -1 : payload;
}



private static int CalculateCrc(int input) {
	const int poly = 0b1011;
	for (int i = 0; i < 9; i++) {
		input = (input & 1) == 1 ? (input >> 1) ^ poly : input >> 1;
	}
	return input & 0b111;
}