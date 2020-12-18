package peasyGradients.colorSpaces;

/***
 * Standard RGB (1996), not to be confused with CIERGB (1931). The RGB color
 * system describes a color based on the amounts of the base colors red, green,
 * and blue. Thus, a particular color has three coordinates, (R,G,B). Each
 * coordinate must be between 0 and 1.
 * 
 * @author micycle1
 *
 */
final class RGB implements ColorSpace {

	RGB() {
	}

	/**
	 * Functionless, but satisfies interface
	 */
	public double[] toRGB(double[] rgb) {
		return rgb;
	}

	/*
	 * Functionless, but satisfies interface
	 */
	public double[] fromRGB(double[] rgb) {
		return rgb;
	}

}
