// MathUtils.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * Handy utility functions which have some Mathematical relavance.
 *
 * @author Matthew Goode
 * @author Alexei Drummon
 *
 * @version $Id: MathUtils.java,v 1.3 2001/07/13 14:39:13 korbinian Exp $
 */


public class MathUtils {

	public MathUtils() {}

	/**
	 * A random number generator that is initialized with the clock when this
	 * class is loaded into the JVM. Use this for all random numbers.
	 */
	public static MersenneTwisterFast random = new MersenneTwisterFast();

	/**
	 * @return a new double array where all the values sum to 1. 
	 * Relative ratios are preserved.
	 */
	public static final double[] getNormalized(double[] array) {
		double[] newArray = new double[array.length];
		double total = getTotal(array);
		for(int i = 0 ; i < array.length ; i++) {
			newArray[i] = array[i]/total;
		}
		return newArray;
	}

	/**
	 * @param end the index of the element after the last one to be included
	 * @return the total of a the values in a range of an array
	 */
	public static final double getTotal(double[] array, int start, int end) {
		double total = 0.0;
		for(int i = start ; i < array.length ; i++) {
			total+=array[i];
		}
		return total;
	}
	
	/**
	 * @return the total of the values in an array
	 */
	public static final double getTotal(double[] array) {
		return getTotal(array,0, array.length);

	}


}

