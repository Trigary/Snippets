private static readonly int[] HammingEncodingIndexes = {
	2, 4, 5, 6, 8, 9, 10, 11
}; //maps input data bit indexes to the hamming-included bit indexes

private static readonly int[] HammingDecodingIndexes = {
	-1, -1, 0, -1, 1, 2, 3, -1, 4, 5, 6, 7
}; //maps the hamming-included data bit indexes to the data-bit value only indexes

private static readonly bool[] HammingIsData = {
	false, false, true, false, true, true, true, false, true, true, true, true
}; //specifies whether the bit in the hamming-included value is a data bit



/// <summary>
/// Computes a hamming code for the specified (8 bits long) value.
/// Returns the value with the hamming code attached or -1, if the input is out of bounds.
/// </summary>
public static int AddHamming(int value) {
	if (value < 0 || value >= 256) {
		return -1;
	}

	int output = 0;
	for (int i = 0; i < 8; i++) {
		if ((value & (1 << i)) != 0) {
			output |= 1 << HammingEncodingIndexes[i]; //0b10 as input -> d2=1
		}
	}

	for (int p = 0; p < 4; p++) {
		int parity = 1 << p;
		bool odd = false;
		for (int i = 0; i < 8; i++) {
			if (((HammingEncodingIndexes[i] + 1) & parity) != 0 && (value & (1 << i)) != 0) {
				odd = !odd;
			}
		}
		if (odd) {
			output |= 1 << (parity - 1);
		}
	}
	return output; //MSB (if this int were 12 bits long) is d8, LSB is p1
}

/// <summary>
/// Removes the hamming code from the (8 bits long) input.
/// Returns the original value or -1, if the input is out of bounds or if the error correction failed.
/// </summary>
public static int RemoveHamming(int value) {
	if (value < 0 || value >= 4096) {
		return -1;
	}
	
	int output = 0;
	int parities = 0;
	for (int i = 11; i >= 0; i--) {
		int bit = (value & (1 << i)) >> i;
		if (HammingIsData[i]) {
			output <<= 1;
			output |= bit;
		} else {
			parities <<= 1;
			parities |= bit;
		}
	}

	for (int p = 0; p < 4; p++) {
		int parity = 1 << p;
		bool odd = false;
		for (int i = 0; i < 8; i++) {
			if (((HammingEncodingIndexes[i] + 1) & parity) != 0 && (output & (1 << i)) != 0) {
				odd = !odd;
			}
		}
		if (odd) {
			parities ^= parity;
		}
	}

	if (parities != 0) {
		if (parities > 12) {
			return -1;
		}
		if (HammingIsData[parities - 1]) {
			output ^= 1 << HammingDecodingIndexes[parities - 1];
		}
	}
	return output;
}
