/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
 *
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Gamma;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class BetaDistribution extends Distribution {
	@Keyword(description = "The alpha tuning parameter.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput alphaInput;

	@Keyword(description = "The beta tuning parameter.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput betaInput;

	@Keyword(description = "The scale parameter for the distribution.  This scales the " +
	                       "value of the distribution so it return values between 0 and scale.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput scaleInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		alphaInput = new SampleInput("AlphaParam", KEY_INPUTS, new SampleConstant(1.0d));
		alphaInput.setUnitType(DimensionlessUnit.class);
		alphaInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(alphaInput);

		betaInput = new SampleInput("BetaParam", KEY_INPUTS, new SampleConstant(1.0d));
		betaInput.setUnitType(DimensionlessUnit.class);
		betaInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(betaInput);

		scaleInput = new SampleInput("Scale", KEY_INPUTS, new SampleConstant(1.0d));
		scaleInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		scaleInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(scaleInput);
	}

	public BetaDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scaleInput.setUnitType(ut);
	}

	@Override
	protected double getSample(double simTime) {
		double alpha = alphaInput.getNextSample(simTime);
		double beta = betaInput.getNextSample(simTime);
		double scale = scaleInput.getNextSample(simTime);
		return getSample(alpha, beta, scale, rng);
	}

	public static double getSample(double alpha, double beta, double scale, MRG1999a rng) {
		// Effectively calculate the inverse CDF
		double val = rng.nextUniform();

		double low = 0;
		double high = 1;
		double guess = 0.5;

		double lowVal = 0;
		double highVal = 1;

		while (true) {
			double attempt = regularizedBeta(guess, alpha, beta, 1E-14,
					Integer.MAX_VALUE);

			if (near(val, attempt, 1E-9)) {
				return guess * scale;
			}

			if (val < attempt) {
				high = guess;
				highVal = attempt;
			} else {
				low = guess;
				lowVal = attempt;
			}

			double ratio = (val - lowVal) / (highVal - lowVal);
			guess = low + (high - low) * ratio;
		}
	}

	@Override
	protected double getMean(double simTime) {
		double alpha = alphaInput.getNextSample(simTime);
		double beta = betaInput.getNextSample(simTime);
		double scale = scaleInput.getNextSample(simTime);
		return getMean(alpha, beta, scale);
	}

	public static double getMean(double alpha, double beta, double scale) {
		return (alpha / (alpha + beta)) * scale;
	}

	@Override
	protected double getStandardDev(double simTime) {
		double alpha = alphaInput.getNextSample(simTime);
		double beta = betaInput.getNextSample(simTime);
		double scale = scaleInput.getNextSample(simTime);
		return getStandardDev(alpha, beta, scale);
	}

	public static double getStandardDev(double alpha, double beta, double scale) {
		double apbSqrd = (alpha + beta) * (alpha + beta);
		return (Math.sqrt(alpha * beta / (apbSqrd * (alpha + beta + 1)))) * scale;
	}

	/*
	 * All code below this point is derived from the beta function implementation of the
	 * Apache Commons Math library and can be downloaded from:
	 * http://commons.apache.org/proper/commons-math/download_math.cgi
	 *
	 */
	public static double regularizedBeta(double x, final double a,
			final double b, double epsilon, int maxIterations) {
		double ret;

		if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b) || x < 0
				|| x > 1 || a <= 0 || b <= 0) {
			ret = Double.NaN;
		} else if (x > (a + 1) / (2 + b + a) && 1 - x <= (b + 1) / (2 + b + a)) {
			ret = 1 - regularizedBeta(1 - x, b, a, epsilon, maxIterations);
		} else {
			ret = Math.exp((a * Math.log(x)) + (b * Math.log1p(-x))
					- Math.log(a) - logBeta(a, b))
					* 1.0 / evaluateFraction(a, b, x, epsilon, maxIterations);
		}

		return ret;
	}

	private static final double HALF_LOG_TWO_PI = .9189385332046727;

	private static final long SGN_MASK = 0x8000000000000000L;

	private static boolean equals(double x, double y, int maxUlps) {
		long xInt = Double.doubleToLongBits(x);
		long yInt = Double.doubleToLongBits(y);

		// Make lexicographically ordered as a two's-complement integer.
		if (xInt < 0) {
			xInt = SGN_MASK - xInt;
		}
		if (yInt < 0) {
			yInt = SGN_MASK - yInt;
		}

		final boolean isEqual = Math.abs(xInt - yInt) <= maxUlps;

		return isEqual && !Double.isNaN(x) && !Double.isNaN(y);
	}

	private static boolean near(double x, double y, double eps) {
		return equals(x, y, 1) || Math.abs(y - x) <= eps;
	}

	private static double getB(double a, double b, int n, double x) {
		double ret;
		double m;
		if (n % 2 == 0) { // even
			m = n / 2.0;
			ret = (m * (b - m) * x) / ((a + (2 * m) - 1) * (a + (2 * m)));
		} else {
			m = (n - 1.0) / 2.0;
			ret = -((a + m) * (a + b + m) * x)
					/ ((a + (2 * m)) * (a + (2 * m) + 1.0));
		}
		return ret;
	}

	// This evaluates and continued fraction for the beta distribution
	// I have no idea how the math actually works though...
	private static double evaluateFraction(double alpha, double beta, double x, double epsilon, int maxIterations) {
		final double small = 1e-50;
		double hPrev = 1.0;

		// use the value of small as epsilon criteria for zero checks
		if (near(hPrev, 0.0, small)) {
			hPrev = small;
		}

		int n = 1;
		double dPrev = 0.0;
		double cPrev = hPrev;

		while (n < maxIterations) {
			final double a = 1.0;
			final double b = getB(alpha, beta, n, x);

			double dN = a + b * dPrev;
			if (near(dN, 0.0, small)) {
				dN = small;
			}
			double cN = a + b / cPrev;
			if (near(cN, 0.0, small)) {
				cN = small;
			}

			dN = 1 / dN;
			final double deltaN = cN * dN;
			final double hN = hPrev * deltaN;

			if (Double.isInfinite(hN)) {
				throw new RuntimeException("Fraction did not converge");
			}
			if (Double.isNaN(hN)) {
				throw new RuntimeException("Fraction did not converge");
			}

			if (Math.abs(deltaN - 1.0) < epsilon) {
				return hN;
			}

			dPrev = dN;
			cPrev = cN;
			hPrev = hN;
			n++;
		}

		throw new RuntimeException("Fraction did not converge");

	}

	public static double logBeta(final double p, final double q) {
		if (Double.isNaN(p) || Double.isNaN(q) || (p <= 0.0) || (q <= 0.0)) {
			return Double.NaN;
		}

		final double a = Math.min(p, q);
		final double b = Math.max(p, q);
		if (a >= 10.0) {
			final double w = sumDeltaMinusDeltaSum(a, b);
			final double h = a / b;
			final double c = h / (1.0 + h);
			final double u = -(a - 0.5) * Math.log(c);
			final double v = b * Math.log1p(h);
			if (u <= v) {
				return (((-0.5 * Math.log(b) + HALF_LOG_TWO_PI) + w) - u) - v;
			} else {
				return (((-0.5 * Math.log(b) + HALF_LOG_TWO_PI) + w) - v) - u;
			}
		} else if (a > 2.0) {
			if (b > 1000.0) {
				final int n = (int) Math.floor(a - 1.0);
				double prod = 1.0;
				double ared = a;
				for (int i = 0; i < n; i++) {
					ared -= 1.0;
					prod *= ared / (1.0 + ared / b);
				}
				return (Math.log(prod) - n * Math.log(b))
						+ (Gamma.logGamma(ared) + logGammaMinusLogGammaSum(
								ared, b));
			} else {
				double prod1 = 1.0;
				double ared = a;
				while (ared > 2.0) {
					ared -= 1.0;
					final double h = ared / b;
					prod1 *= h / (1.0 + h);
				}
				if (b < 10.0) {
					double prod2 = 1.0;
					double bred = b;
					while (bred > 2.0) {
						bred -= 1.0;
						prod2 *= bred / (ared + bred);
					}
					return Math.log(prod1)
							+ Math.log(prod2)
							+ (Gamma.logGamma(ared) + (Gamma.logGamma(bred) - logGammaSum(
									ared, bred)));
				} else {
					return Math.log(prod1) + Gamma.logGamma(ared)
							+ logGammaMinusLogGammaSum(ared, b);
				}
			}
		} else if (a >= 1.0) {
			if (b > 2.0) {
				if (b < 10.0) {
					double prod = 1.0;
					double bred = b;
					while (bred > 2.0) {
						bred -= 1.0;
						prod *= bred / (a + bred);
					}
					return Math.log(prod)
							+ (Gamma.logGamma(a) + (Gamma.logGamma(bred) - logGammaSum(
									a, bred)));
				} else {
					return Gamma.logGamma(a) + logGammaMinusLogGammaSum(a, b);
				}
			} else {
				return Gamma.logGamma(a) + Gamma.logGamma(b)
						- logGammaSum(a, b);
			}
		} else {
			if (b >= 10.0) {
				return Gamma.logGamma(a) + logGammaMinusLogGammaSum(a, b);
			} else {
				// The following command is the original NSWC implementation.
				// return Gamma.logGamma(a) +
				// (Gamma.logGamma(b) - Gamma.logGamma(a + b));
				// The following command turns out to be more accurate.
				return Math.log(com.jaamsim.math.Gamma.gamma(a)
						* Gamma.gamma(b) / Gamma.gamma(a + b));
			}
		}
	}

	private static double logGammaSum(final double a, final double b) {

		final double x = (a - 1.0) + (b - 1.0);
		if (x <= 0.5) {
			return Gamma.logGamma1p(1.0 + x);
		} else if (x <= 1.5) {
			return Gamma.logGamma1p(x) + Math.log1p(x);
		} else {
			return Gamma.logGamma1p(x - 1.0) + Math.log(x * (1.0 + x));
		}
	}

	private static double logGammaMinusLogGammaSum(final double a,
			final double b) {
		/*
		 * d = a + b - 0.5
		 */
		final double d;
		final double w;
		if (a <= b) {
			d = b + (a - 0.5);
			w = deltaMinusDeltaSum(a, b);
		} else {
			d = a + (b - 0.5);
			w = deltaMinusDeltaSum(b, a);
		}

		final double u = d * Math.log1p(a / b);
		final double v = a * (Math.log(b) - 1.0);

		return u <= v ? (w - u) - v : (w - v) - u;
	}

	private static final double[] DELTA = {
			.833333333333333333333333333333E-01,
			-.277777777777777777777777752282E-04,
			.793650793650793650791732130419E-07,
			-.595238095238095232389839236182E-09,
			.841750841750832853294451671990E-11,
			-.191752691751854612334149171243E-12,
			.641025640510325475730918472625E-14,
			-.295506514125338232839867823991E-15,
			.179643716359402238723287696452E-16,
			-.139228964661627791231203060395E-17,
			.133802855014020915603275339093E-18,
			-.154246009867966094273710216533E-19,
			.197701992980957427278370133333E-20,
			-.234065664793997056856992426667E-21,
			.171348014966398575409015466667E-22 };

	private static double deltaMinusDeltaSum(final double a, final double b) {

		final double h = a / b;
		final double p = h / (1.0 + h);
		final double q = 1.0 / (1.0 + h);
		final double q2 = q * q;
		/*
		 * s[i] = 1 + q + ... - q**(2 * i)
		 */
		final double[] s = new double[DELTA.length];
		s[0] = 1.0;
		for (int i = 1; i < s.length; i++) {
			s[i] = 1.0 + (q + q2 * s[i - 1]);
		}
		/*
		 * w = Delta(b) - Delta(a + b)
		 */
		final double sqrtT = 10.0 / b;
		final double t = sqrtT * sqrtT;
		double w = DELTA[DELTA.length - 1] * s[s.length - 1];
		for (int i = DELTA.length - 2; i >= 0; i--) {
			w = t * w + DELTA[i] * s[i];
		}
		return w * p / b;
	}

	private static double sumDeltaMinusDeltaSum(final double p, final double q) {

		final double a = Math.min(p, q);
		final double b = Math.max(p, q);
		final double sqrtT = 10.0 / a;
		final double t = sqrtT * sqrtT;
		double z = DELTA[DELTA.length - 1];
		for (int i = DELTA.length - 2; i >= 0; i--) {
			z = t * z + DELTA[i];
		}
		return z / a + deltaMinusDeltaSum(a, b);
	}

}
