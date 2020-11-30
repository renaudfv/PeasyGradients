package peasyGradients.gradient;

import java.util.ArrayList;
import java.util.Iterator;

import net.jafama.FastMath;
import peasyGradients.colorSpaces.*;
import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;
import peasyGradients.utilities.Interpolation;
import processing.core.PApplet;

/**
 * A Gradient comprises of {@link peasyGradients.gradient.ColorStop color stops}
 * (each specifying a color and a position) arranged on a 1D axis. Gradients
 * define the gradient curve function (such as sine()) -- the function governing
 * how the gradient's color transtions (the step used during interpolation) and
 * the color space this gradient will be rendered this gradient's color stops
 * are represented by.
 * 
 * <p>
 * Use a {@link #peasyGradients.PeasyGradients} instance to render Gradients.
 * 
 * 
 * @author micycle1
 *
 */
public final class Gradient {
	
	 // TODO export as JSON / load from JSON

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();

	private double[] interpolatedcolorOUT = new double[4]; // define once here

	private float offset = 0; // animation color offset 0...1

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private float denom;

//	double[] colorOut; // TODO color (in Gradient's current colorspace)

	public ColorSpaces colorSpace = ColorSpaces.LUV; // TODO public for testing
	ColorSpace colorSpaceInstance = colorSpace.getColorSpace(); // call toRGB on this instance
	public Interpolation interpolationMode = Interpolation.SMOOTH_STEP; // TODO public/

	/**
	 * Constructs a new gradient consisting of 2 equidistant complementary colors.
	 */
	public Gradient() {
		this(Palette.complementary()); // random 2 colors
	}

	/**
	 * Creates a gradient with equidistant color stops.
	 * 
	 * @param colors ARGB color integers (the kind returned by Processing's color()
	 *               method)
	 */
	public Gradient(int... colors) {
		int sz = colors.length;
		float szf = (sz <= 1.0f ? 1.0f : sz - 1.0f);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colors[i], i / szf));
		}
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/*
	 * TODO
	 */
	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/*
	 * TODO
	 */
	public Gradient(ArrayList<ColorStop> colorStops) {
		this.colorStops = colorStops;
		java.util.Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/**
	 * Sets the color of a color stop (given by its index).
	 * 
	 * @param stopIndex
	 * @param col       ARGB color integer representation
	 */
	public void setStopColor(int stopIndex, int col) {
		if (stopIndex > -1 && stopIndex < colorStops.size()) {
			colorStops.get(stopIndex).setcolor(col);
		} else {
			System.err.println("Color stop index out of bounds.");
		}
	}

	/**
	 * TODO Sets the 1D position of a color stop (given by its index) to a certain
	 * position on the 1D gradient axis. Positions < 0 or > 1 will wrap around the
	 * gradient.
	 * 
	 * @param index
	 * @param position
	 */
	public void setStopPosition(int index, float position) {
		if (index > -1 && index < colorStops.size()) {
			colorStops.get(index).setPosition(position);
			java.util.Collections.sort(colorStops);
		} else {
			System.err.println("Color stop index out of bounds.");
		}
	}

	/**
	 * Increases the positional offset of all color stops by the amount given (call
	 * this each frame (within draw() for example) to animate a gradient).
	 * 
	 * @param amt 0...1 smaller is less change
	 * @see #setOffset(float)
	 * @see #primeAnimation() primeAnimation() -- consider calling this method
	 *      before animate() to prevent a color seam
	 */
	public void animate(float amt) {
		offset += amt;
	}

	/**
	 * Sets the offset of <b>all color stops</b> to a specific value.
	 * 
	 * @param offset
	 * @see #animate(float)
	 */
	public void setOffset(float offset) {
		this.offset = offset;
	}

	/**
	 * Mutates the color of all color stops in the RGB255 space by the amount given.
	 * Mutation randomises between adding or subtracting the mutation amount from
	 * each of the R,G,B channels.
	 * 
	 * @param amt magnitude of mutation [0...255]
	 */
	public void mutatecolor(float amt) {
		colorStops.forEach(c -> c.mutate(amt));
	}

	/**
	 * Primes the gradient for animation (pushes copy of the first color in the
	 * gradient to the end, and repositions all other color stops proportionally to
	 * where they were before), to ensure a seamless gradient spectrum, regardless
	 * of offset.
	 * 
	 * <p>
	 * Animating a gradient without calling {@link #primeAnimation()} may lead to an
	 * ugly and undesirable seam in the gradient where the first and last color
	 * stops (at positions 0.00 and 1.00 respectively) bump right up against each
	 * other.
	 */
	public void primeAnimation() {
		push(colorAt(0));
	}

	/**
	 * Pushes a new color stop to the end of the gradient (position = 1.00), and
	 * repositions all other color stops proportionally to where they were before.
	 * 
	 * @param color
	 * @see #removeLast()
	 */
	public void push(int color) {
		for (ColorStop colorStop : colorStops) {
			colorStop.position *= ((colorStops.size() - 1) / (float) colorStops.size()); // scale down existing stop positions
		}
		add(1, color);
	}

	/**
	 * Removes the last color stop from the gradient and scale the rest to fill the
	 * gradient. TODO test
	 * 
	 * @see #push(int)
	 */
	public void removeLast() {
		colorStops.remove(colorStops.size() - 1);
		// scale up remaining stop positions
		for (ColorStop colorStop : colorStops) {
			colorStop.position *= ((colorStops.size()) / (float) (colorStops.size() - 1f)); // scale down existing stop positions
		}
	}

	/**
	 * Returns the ARGB color of the color stop at a given index.
	 * 
	 * @param colorStopIndex
	 * @return 32bit ARGB color int
	 */
	public int colorAt(int colorStopIndex) {
		return colorStops.get(colorStopIndex).clr;
	}

	/**
	 * Returns this gradient's last color.
	 * 
	 * @return 32bit ARGB color int
	 */
	public int lastcolor() {
		return colorStops.get(colorStops.size() - 1).clr;
	}

	/**
	 * Adds a specific color to the gradient at a given percentage.
	 * 
	 * @param percent 0...1
	 * @param clr
	 */
	public void add(final float percent, final int clr) {
		add(new ColorStop(clr, percent));
	}

	/**
	 * Adds a color stop to the gradient.
	 * 
	 * @param colorStop
	 */
	public void add(final ColorStop colorStop) {
		colorStops.add(colorStop);
		java.util.Collections.sort(colorStops); // sort color stops by value
	}

	/**
	 * Primes for rendering (TODO protected?)
	 */
	public void prime() {
		lastCurrStopIndex = 0;
		currStop = colorStops.get(lastCurrStopIndex);
		prevStop = colorStops.get(0);
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/**
	 * Evalutes the ARGB color value of the gradient at the given step through the
	 * 1D axis (0.0...1.0).
	 * 
	 * <p>
	 * This is the main method of Gradient class. Internally, the the position input
	 * undergoes is transformed by the current interpolation function.
	 * 
	 * @param position a linear position expressed as a decimal between numbers
	 *                 outside the range of 0...1 will wrap back into the gradient
	 * @return ARGB integer for Processing pixel array.
	 */
	public int getColor(float position) {

		position += offset;
		if (position < 0) { // (if animation offset negative)
			position += 1; // equivalent to floormod function
		}
		if (position > 1) { // 1 % 1 == 0, which we want to avoid
			position %= 1;
		}

		/**
		 * Calculate whether the current step has gone beyond the existing color stop
		 * boundary (either above or below). If the first color stop is at a position >
		 * 0 or last color stop at a position < 1, then when step > currStop.percent or
		 * step < currStop.percent is true, we don't want to inc/decrement currStop.
		 */
		if (position > currStop.position) { // if at end, stay, otherwise next
			if (lastCurrStopIndex == (colorStops.size() - 1)) {
				prevStop = colorStops.get(lastCurrStopIndex);
				denom = 1;
			} else {
				do {
					lastCurrStopIndex++; // increment
					currStop = colorStops.get(lastCurrStopIndex);
					// sometimes step might jump more than 1 color, hence while()
				} while (position > currStop.position && lastCurrStopIndex < (colorStops.size() - 1));
				prevStop = colorStops.get(lastCurrStopIndex - 1);

				denom = 1 / (prevStop.position - currStop.position); // compute denominator inverse
			}

		} else if (position <= prevStop.position) {
			if (lastCurrStopIndex == 0) { // if at zero stay, otherwise prev
				denom = 1;
				currStop = colorStops.get(0);
			} else {
				do {
					lastCurrStopIndex--; // decrement
					prevStop = colorStops.get(Math.max(lastCurrStopIndex - 1, 0));
				} while (position < prevStop.position); // sometimes step might jump back more than 1 color

				currStop = colorStops.get(lastCurrStopIndex);

				denom = 1 / (prevStop.position - currStop.position); // compute denominator inverse
			}
		}

		/**
		 * A simpler approach. Since we pre-compute results into a LUT, this function
		 * works with monotonically increasing step. HOWEVER, doesn't work if animating,
		 * hence commented out.
		 */
//		if (step > currStop.percent && lastCurrStopIndex != (colorStops.size() - 1) ) {
//			prevStop = colorStops.get(lastCurrStopIndex);
//			lastCurrStopIndex++; // increment
//			currStop = colorStops.get(lastCurrStopIndex);
//			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
//		}

		double smoothStep = functStep((position - currStop.position) * denom); // apply interpolation function

		// interpolate within given colorspace
		colorSpaceInstance.interpolateLinear(currStop.colorOut, prevStop.colorOut, smoothStep, interpolatedcolorOUT);
		int alpha = (int) Math.floor((currStop.alpha + (position * (prevStop.alpha - currStop.alpha))) + 0.5d);

		// convert current colorspace value to sARGB int and return
		return Functions.composeclr(colorSpaceInstance.toRGB(interpolatedcolorOUT), alpha);
	}

	/**
	 * TODO color space is defined for user at peasyGradients level, not gradient
	 * (1D)?
	 * 
	 * @param colorSpace
	 */
	public void setColorSpace(ColorSpaces colorSpace) {
		this.colorSpace = colorSpace;
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public void nextColSpace() {
		colorSpace = colorSpace.next();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public void prevColSpace() {
		colorSpace = colorSpace.prev();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public void setInterpolationMode(Interpolation interpolationMode) {
		this.interpolationMode = interpolationMode;
	}

	public void nextInterpolationMode() {
		interpolationMode = interpolationMode.next();
	}

	public void prevInterpolationMode() {
		interpolationMode = interpolationMode.prev();
	}

	boolean remove(ColorStop colorStop) {
		return colorStops.remove(colorStop);
	}

	ColorStop remove(int i) {
		return colorStops.remove(i);
	}

	/**
	 * Return randomised gradient (random colors and stop positions). TODO use
	 * pallete?
	 * 
	 * @param numcolors
	 * @return
	 */
	public static Gradient randomGradientWithStops(int numcolors) {
		ColorStop[] temp = new ColorStop[numcolors];
		float percent;
		for (int i = 0; i < numcolors; ++i) {
			percent = i == 0 ? 0 : i == numcolors - 1 ? 1 : (float) Math.random();
			temp[i] = new ColorStop(Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1), percent);
		}
		return new Gradient(temp);
	}

	/**
	 * Return randomised gradient (random colors; equidistant stops). TODO use
	 * palette instead?
	 * 
	 * @param numcolors
	 * @return
	 */
	public static Gradient randomGradient(int numcolors) {
		int[] temp = new int[numcolors];
		for (int i = 0; i < numcolors; ++i) {
			temp[i] = Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);
		}
		return new Gradient(temp);
	}

	/**
	 * Returns detailed information about the gradient. For each color stop,
	 * returns:
	 * <p>
	 * <ul>
	 * <li>Position
	 * <li>RGBA representation
	 * <li>Integer representation
	 * <li>[Current color Space] Representation
	 * </ul>
	 * <p>
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient\n");
		sb.append("Offset: " + offset + "\n");
		sb.append("color Stops (" + colorStops.size() + "):\n");
		sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", "Position", "RGBA", "clrInteger", "clrCurrentColSpace"));
		for (ColorStop colorStop : colorStops) {
			sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", colorStop.position,
					Functions.formatArray(Functions.decomposeclrRGBDouble(colorStop.clr, colorStop.alpha), 0, 2), colorStop.clr,
					Functions.formatArray(colorStop.getcolor(colorSpace), 2, 3)));
		}
		return sb.toString();
	}

	/**
	 * Returns Java source to paste. Call this method if a randomly generated
	 * gradient is pleasant. Export ready to construct the gradient using Processing
	 * color().
	 * 
	 * @return e.g. "Gradient(color(0, 0, 50), color(125, 55, 25));"
	 */
	public String toJavaConstructor() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient(");

		Iterator<ColorStop> iterator = colorStops.iterator();
		ColorStop c = iterator.next();
		sb.append("color" + Functions.formatArray(Functions.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));

		while (iterator.hasNext()) {
			c = iterator.next();
			sb.append(", ");
			sb.append("color" + Functions.formatArray(Functions.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));
		}

		sb.append(");");
		return sb.toString();
	}

	/**
	 * Calculates the eased step by passing the original (linear) step to the
	 * Gradient's current interpolation function. Allows gradient renderer to easily
	 * change how the gradient is smoothed.
	 * 
	 * @param step 0...1
	 * @return the eased/transformed step (0...1)
	 */
	private double functStep(float step) {
		switch (interpolationMode) {
			case LINEAR :
				return step;
			case IDENTITY :
				return step * step * (2.0f - step);
			case SMOOTH_STEP :
				return 3 * step * step - 2 * step * step * step; // polynomial approximation of (0.5-cos(PI*step)/2)
			case SMOOTHER_STEP :
				return step * step * step * (step * (step * 6 - 15) + 10);
			case EXPONENTIAL :
				return step == 1.0f ? step : 1.0f - FastPow.fastPow(2, -10 * step);
			case CUBIC :
				return step * step * step;
			case BOUNCE :
				float sPrime = step;

				if (sPrime < 0.36364) { // 1/2.75
					return 7.5625f * sPrime * sPrime;
				}
				if (sPrime < 0.72727) // 2/2.75
				{
					return 7.5625f * (sPrime -= 0.545454f) * sPrime + 0.75f;
				}
				if (sPrime < 0.90909) // 2.5/2.75
				{
					return 7.5625f * (sPrime -= 0.81818f) * sPrime + 0.9375f;
				}
				return 7.5625f * (sPrime -= 0.95455f) * sPrime + 0.984375f;
			case CIRCULAR :
				return Math.sqrt((2.0 - step) * step);
			case SINE :
				return FastMath.sinQuick(step);
			case PARABOLA :
				return Math.sqrt(4.0 * step * (1.0 - step));
			case GAIN1 :
				if (step < 0.5f) {
					return 0.5f * FastPow.fastPow(2.0f * step, 0.3f);
				} else {
					return 1 - 0.5f * FastPow.fastPow(2.0f * (1 - step), 0.3f);
				}
			case GAIN2 :
				if (step < 0.5f) {
					return 0.5f * FastPow.fastPow(2.0f * step, 3.3333f);
				} else {
					return 1 - 0.5f * FastPow.fastPow(2.0f * (1 - step), 3.3333f);
				}
			case EXPIMPULSE :
				return (2 * step * FastMath.expQuick(1.0 - (2 * step)));
			default :
				return step;
		}
	}

}
