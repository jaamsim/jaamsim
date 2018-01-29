/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jaamsim.RunControlObjects;

// Apache-Commons-Math is required:
// http://commons.apache.org/proper/commons-math/
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;
import com.jaamsim.datatypes.DoubleVector;

// TODO validation?

/**
 *  Stores functions relating to statistics, e.g t-coefficients.
 *
 *  @author Michael Bergman
 */
public class StatsUtils {

	/**
	 *
	 * To calculate the t-statistic for a given p-value and number of items
	 * For a 2 tail p value, t-test
	 *
	 *  @param p the p-value
	 *  @param n number of samples (I.E. Degrees of freedom + 1)
	 *
	 *  @return The t-statistic
	 *
	 */
	public static double tStat2Tail(double p, int n) {
		assert(0 <= p);
		assert(p < 1);
		assert(1 < n);

		double out = 0f;
		double degFree = n-1.0;

		p /= 2;
		// T distribution
		TDistribution T = new TDistribution(null, degFree, 1e-9);
		out = -T.inverseCumulativeProbability(p);	// as the distribution is symmetric
		return out;
	}


	/**
	 *
	 * To calculate the t-statistic for a given p-value and number of items
	 * For a 1 tail value
	 *
	 * 	@param p the p-value
	 *  @param n number of samples (Degrees of freedom + 1)
	 *
	 *  @return The t-statistic
	 *
	 */
	public static double tStat1Tail(double p, int n) {
		assert(0 < p);
		assert(p < 1);
		assert(1 < n);

		double out = 0f;
		double degFree = n-1.0;

		// T distribution
		TDistribution T = new TDistribution(null, degFree, 1e-9);
		out = T.inverseCumulativeProbability(p);
		return out;
	}

	/**
	 *
	 * @param p the probability that x < Z for some x chosen at random according to the normal distribution
	 *
	 * @return the Z value
	 *
	 */
	public static double inverseCummulativeNormal(double p) {
		assert(0 < p && p < 1);
		return (new NormalDistribution()).inverseCumulativeProbability(p);
	}

	/**
	 *
	 * Calculates the auto-covariance for an array, with all elements having the
	 * same weight.
	 *
	 * @param X The list of samples
	 * @param s the distance between elements of the sequence, for the auto-covariance
	 *
	 * @return The auto-covariance
	 *
	 */
	public static double AutoCovariance(DoubleVector X, int s) {

		assert (s >= 0);
		/*TODO: length check on X to make sure there are enough entries
		 *		for the given s?
		*/

		double size = X.size();
		double muX = 0.0d;
		double autocov = 0.0d;

		muX = X.sum() / size;

		for(int i = 0; i < size - s ; i++) {
			autocov += (X.get(i) - muX)*(X.get(i+s) - muX);
		}

		autocov /= size;

		return autocov;
	}

	/**
	 *
	 * @param X The vector of sample values
	 * @param s the distance between elements of the sequence, in the calculation of autocorrelation
	 *
	 * @return
	 *
	 */
	public static double AutoCorrelation(DoubleVector X, int s) {

		assert (s > 0);
		/*TODO: length check on X to make sure there are enough entries
		 *		for the given s
		*/

		double size = X.size();
		double muX = 0.0d;
		double varX = 0.0d;
		double autocorr = 0.0d;

		// calculate the means
		for(int i = 0; i < size ; i++) {
			muX += X.get(i);
			varX += X.get(i)*X.get(i);
		}
		muX /= size;
		varX /= size-1;

		for(int i = 0; i < size - s ; i++) {
			autocorr += (X.get(i) - muX)*(X.get(i+s) - muX);
		}

		autocorr /= size;
		autocorr /= varX;

		return autocorr;

	}


	/**
	 *
	 *  Fishman 1978 test for grouping observations in digital simulations
	 * 	Test to test whether we can safely assume that the sample means are
	 *  independent.
	 *
	 *  Assume Central Limit Theorem holds here
	 *
	 * @param vals The vector of sample values
	 * @param beta The p value, assuming the normal distribution is valid
	 *
	 * @return
	 *
	 */
	public static boolean isSampleCorrelated(DoubleVector vals, double beta) {

		if ((beta <= 0) || (beta >= 0.5)) throw new OutOfRangeException(beta, 0.0, 0.5);

		double k = vals.size();

		if(k <= 8.0) {
			return true;
		}

		// mean
		double muX = vals.sum()/k;
		// variance then correlation
		double R0 = AutoCovariance(vals, 0);
		double P1 = AutoCovariance(vals, 1) / R0;

		double fst = vals.get(0) - muX;
		double lst = vals.lastElement() - muX;

		double cK = P1 + (fst*fst + lst*lst) / (2*k*R0);

		double Z = inverseCummulativeNormal(1 - beta);
		double RHS = Z*Math.sqrt( (k - 2) / (k*k - 1) );

		return cK > RHS;
	}

	/**
	 *
	 * isSampleCorrelated, with beta := 0.05
	 *
	 * @param vals The vector of sample values
	 *
	 * @return
	 *
	 */
	public static boolean isSampleCorrelated(DoubleVector vals) {

		int k = vals.size();

		if(k <= 8) {

			return true;
		}

		// mean
		double muX = vals.sum() / k;
		//variance then correlation
		double R0 = AutoCovariance(vals, 0);
		double P1 = AutoCovariance(vals, 1) / R0;

		double fst = vals.get(0) - muX;
		double lst = vals.lastElement() - muX;

		double cK = P1 + ( fst*fst + lst*lst ) / (2*k*R0);
		double RHS = 1.645 * Math.sqrt( (1d*k - 2) / (k*k - 1) );

		return cK > RHS;

	}

	/**
	 *
	 * Calculates the half width interval, this code includes calculation of the t-statistic
	 * and this may have an effect on performance critical code.
	 *
	 * @param vals The vector of sample values
	 * @param alpha
	 *
	 * @return The half width interval of the input sample values
	 *
	 */
	public static double batchHalfWidthInterval(DoubleVector vals, double alpha) {

		int k = vals.size();
		double R0 = AutoCovariance(vals, 0);
		double t = tStat2Tail(alpha, k);
		return t * Math.sqrt(R0/(k-1));

	}

}
