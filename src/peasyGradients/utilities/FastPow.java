package peasyGradients.utilities;

import net.jafama.FastMath;
import peasyGradients.utilities.fastLog.DFastLog;
import peasyGradients.utilities.fastLog.FastLog;
import peasyGradients.utilities.fastLog.TurboLog;

/**
 * Java implementation of 'Fast pow() With Adjustable Accuracy' by Harrison
 * Ainsworth from
 * http://www.hxa7241.org/articles/content/fast-pow-adjustable_hxa7241_2007.html
 * <p>
 * When precision = 11 (8KB table), mean error is < 0.01%, and max error is <
 * 0.02% (proportional, ie: abs(true - approx) / true).
 * <p>
 * The essential approximation is a ‘staircase’ function across the fraction
 * range between successive integer powers. It has full float precision y
 * values, but at limited regular x intervals. Accuracy is proportional to
 * number of table elements.
 * <p>
 * This solution has a small weakness: for radix two it produces inexact results
 * for integer powers when it need not.
 * 
 * @author micycle1
 *
 */
public final class FastPow {

	private static final float _2p23 = 8388608.0f;
	private static final float _2p23b = (127.0f * _2p23);
	private static final float ln2_INV = (float) (1 / Math.log(2));
	private static FastLog fastLog;

	private static int[] table;
	private static int precision;

	/**
	 * Initialize powFast lookup table. Must be called once before use.
	 * 
	 * @param precision number of mantissa bits used, >= 0 and <= 18
	 */
	public static void init(int precision) {

		FastPow.precision = precision;

		table = new int[(int) Math.pow(2, precision)];

		float zeroToOne = 1.0f / ((float) (1 << precision) * 2.0f);
		for (int i = 0; i < (1 << precision); ++i) {
			/* make y-axis value for table element */
			final float f = ((float) Math.pow(2.0f, zeroToOne) - 1.0f) * _2p23;
			table[i] = (int) (f < _2p23 ? f : (_2p23 - 1.0f));

			zeroToOne += 1.0f / (float) (1 << precision);
		}

		fastLog = new DFastLog(precision);
//		fastLog = new TurboLog(precision);

	}

	/**
	 * Use {@link #getBaseRepresentation(float)}
	 * 
	 * @param baseRepresentation one over log, to required radix, of two
	 * @param exponent           power to raise radix to
	 * @return
	 * @see #fastPow(double, double)
	 */
	public static float fastPowConstantBase(final float baseRepresentation, final float exponent) {
		final int i = (int) ((exponent * (_2p23 * baseRepresentation)) + (127.0f * _2p23));

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	public static float fastPow(final float base, final float exponent) {
		final int i = (int) ((exponent * (_2p23 * fastLog.fastLog2(base))) + _2p23b);

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	/**
	 * Includes further optimisation to calculate base representation.
	 * 
	 * @param baseRepresentation the exact base
	 * @param exponent           power to raise radix to
	 * @return
	 * @see #fastPow(float, float)
	 */
	public static float fastPow(final double base, final double exponent) {

		final int i = (int) (exponent * (_2p23 * fastLog.fastLog2(base)) + _2p23b);

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	/**
	 * Calcuate representation of the given radix for use in
	 * {@link #fastPow(float, float)}.
	 * 
	 * @param radix
	 * @return
	 * @see #fastPow(float, float)
	 */
	public static float getBaseRepresentation(float radix) {
		return (float) FastMath.log(radix) * ln2_INV;
	}

}
