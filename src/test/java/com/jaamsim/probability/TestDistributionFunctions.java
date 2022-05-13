/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.probability;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.RunListener;
import com.jaamsim.basicsim.SimRun;

public class TestDistributionFunctions implements RunListener {

	JaamSimModel simModel;
	CountDownLatch countDownLatch = new CountDownLatch(1);

	@Test
	public void testFunctions() {
		simModel = new JaamSimModel();
		simModel.autoLoad();
		simModel.setInput("Simulation", "RunDuration", "10 s");

		simModel.defineEntity("SimEntity", "Proto");
		simModel.defineEntity("EntityGenerator", "Gen");
		simModel.defineEntity("Assign", "Assign1");
		simModel.defineEntity("EntitySink", "Sink");

		simModel.setInput("Gen", "PrototypeEntity", "Proto");
		simModel.setInput("Gen", "InterArrivalTime", "1 s");
		simModel.setInput("Gen", "MaxNumber", "10");
		simModel.setInput("Gen", "NextComponent", "Assign1");
		simModel.setInput("Assign1", "NextComponent", "Sink");

		simModel.defineEntity("BetaDistribution", "Beta");
		simModel.setInput("Beta", "UnitType", "DimensionlessUnit");
		simModel.setInput("Beta", "RandomSeed", "0");
		simModel.setInput("Beta", "AlphaParam", "2");
		simModel.setInput("Beta", "BetaParam", "1");
		simModel.setInput("Beta", "Scale", "1");

		simModel.defineEntity("BinomialDistribution", "Binomial");
		simModel.setInput("Binomial", "UnitType", "DimensionlessUnit");
		simModel.setInput("Binomial", "RandomSeed", "0");
		simModel.setInput("Binomial", "NumberOfTrials", "10");
		simModel.setInput("Binomial", "Probability", "0.4");

		simModel.defineEntity("DiscreteUniformDistribution", "DiscreteUniform");
		simModel.setInput("DiscreteUniform", "UnitType", "DimensionlessUnit");
		simModel.setInput("DiscreteUniform", "RandomSeed", "0");
		simModel.setInput("DiscreteUniform", "MinValue", "3");
		simModel.setInput("DiscreteUniform", "MaxValue", "8");

		simModel.defineEntity("ErlangDistribution", "Erlang");
		simModel.setInput("Erlang", "UnitType", "DimensionlessUnit");
		simModel.setInput("Erlang", "RandomSeed", "0");
		simModel.setInput("Erlang", "Mean", "10");
		simModel.setInput("Erlang", "Shape", "2");

		simModel.defineEntity("ExponentialDistribution", "Exponential");
		simModel.setInput("Exponential", "UnitType", "DimensionlessUnit");
		simModel.setInput("Exponential", "RandomSeed", "0");
		simModel.setInput("Exponential", "Mean", "10");

		simModel.defineEntity("GammaDistribution", "Gamma");
		simModel.setInput("Gamma", "UnitType", "DimensionlessUnit");
		simModel.setInput("Gamma", "RandomSeed", "0");
		simModel.setInput("Gamma", "Mean", "10");
		simModel.setInput("Gamma", "Shape", "2");

		simModel.defineEntity("GeometricDistribution", "Geometric");
		simModel.setInput("Geometric", "UnitType", "DimensionlessUnit");
		simModel.setInput("Geometric", "RandomSeed", "0");
		simModel.setInput("Geometric", "Probability", "0.4");

		simModel.defineEntity("LogLogisticDistribution", "LogLogistic");
		simModel.setInput("LogLogistic", "UnitType", "DimensionlessUnit");
		simModel.setInput("LogLogistic", "RandomSeed", "0");
		simModel.setInput("LogLogistic", "Scale", "10");
		simModel.setInput("LogLogistic", "Shape", "4");

		simModel.defineEntity("LogNormalDistribution", "LogNormal");
		simModel.setInput("LogNormal", "UnitType", "DimensionlessUnit");
		simModel.setInput("LogNormal", "RandomSeed", "0");
		simModel.setInput("LogNormal", "Scale", "1");
		simModel.setInput("LogNormal", "NormalMean", "1");
		simModel.setInput("LogNormal", "NormalStandardDeviation", "0.2");

		simModel.defineEntity("NegativeBinomialDistribution", "NegativeBinomial");
		simModel.setInput("NegativeBinomial", "UnitType", "DimensionlessUnit");
		simModel.setInput("NegativeBinomial", "RandomSeed", "0");
		simModel.setInput("NegativeBinomial", "SuccessfulTrials", "3");
		simModel.setInput("NegativeBinomial", "Probability", "0.4");

		simModel.defineEntity("NormalDistribution", "Normal");
		simModel.setInput("Normal", "UnitType", "DimensionlessUnit");
		simModel.setInput("Normal", "RandomSeed", "0");
		simModel.setInput("Normal", "Mean", "10");
		simModel.setInput("Normal", "StandardDeviation", "2");

		simModel.defineEntity("PoissonDistribution", "Poisson");
		simModel.setInput("Poisson", "UnitType", "DimensionlessUnit");
		simModel.setInput("Poisson", "RandomSeed", "0");
		simModel.setInput("Poisson", "Mean", "10");

		simModel.defineEntity("TriangularDistribution", "Triangular");
		simModel.setInput("Triangular", "UnitType", "DimensionlessUnit");
		simModel.setInput("Triangular", "RandomSeed", "0");
		simModel.setInput("Triangular", "MinValue", "2");
		simModel.setInput("Triangular", "Mode", "4");
		simModel.setInput("Triangular", "MaxValue", "5");

		simModel.defineEntity("UniformDistribution", "Uniform");
		simModel.setInput("Uniform", "UnitType", "DimensionlessUnit");
		simModel.setInput("Uniform", "RandomSeed", "0");
		simModel.setInput("Uniform", "MinValue", "2");
		simModel.setInput("Uniform", "MaxValue", "5");

		simModel.defineEntity("WeibullDistribution", "Weibull");
		simModel.setInput("Weibull", "UnitType", "DimensionlessUnit");
		simModel.setInput("Weibull", "RandomSeed", "0");
		simModel.setInput("Weibull", "Scale", "10");
		simModel.setInput("Weibull", "Shape", "2");

		simModel.setInput("Assign1", "AttributeDefinitionList",
				  "{ diffBeta        0 }"
				+ "{ diffBinomial    0 }"
				+ "{ diffDiscreteUniform  0 }"
				+ "{ diffErlang      0 }"
				+ "{ diffExponential 0 }"
				+ "{ diffGamma       0 }"
				+ "{ diffGeometric   0 }"
				+ "{ diffLogLogistic 0 }"
				+ "{ diffLogNormal   0 }"
				+ "{ diffNegativeBinomial  0 }"
				+ "{ diffNormal      0 }"
				+ "{ diffPoisson     0 }"
				+ "{ diffTriangular  0 }"
				+ "{ diffUniform     0 }"
				+ "{ diffWeibull     0 }"
		);
		simModel.setInput("Assign1", "AttributeAssignmentList",
				  "{ 'this.diffBeta        = this.diffBeta          + beta(2, 1, 1, 0)         - [Beta].Value'        }"
				+ "{ 'this.diffBinomial    = this.diffBinomial      + binomial(10, 0.4, 0)     - [Binomial].Value'    }"
				+ "{ 'this.diffDiscreteUniform = this.diffDiscreteUniform + discreteUniform(3, 8, 0) - [DiscreteUniform].Value' }"
				+ "{ 'this.diffErlang      = this.diffErlang        + erlang(10, 2, 0)         - [Erlang].Value'      }"
				+ "{ 'this.diffExponential = this.diffExponential   + exponential(10, 0)       - [Exponential].Value' }"
				+ "{ 'this.diffGamma       = this.diffGamma         + gamma(10, 2, 0)          - [Gamma].Value'       }"
				+ "{ 'this.diffGeometric   = this.diffGeometric     + geometric(0.4, 0)        - [Geometric].Value'    }"
				+ "{ 'this.diffLogLogistic = this.diffLogLogistic   + loglogistic(10, 4, 0)    - [LogLogistic].Value' }"
				+ "{ 'this.diffLogNormal   = this.diffLogNormal     + lognormal(1, 1, 0.2, 0)  - [LogNormal].Value'   }"
				+ "{ 'this.diffNegativeBinomial = this.diffNegativeBinomial + negativeBinomial(3, 0.4, 0) - [NegativeBinomial].Value'    }"
				+ "{ 'this.diffNormal      = this.diffNormal        + normal(10, 2, 0)         - [Normal].Value'      }"
				+ "{ 'this.diffPoisson     = this.diffPoisson       + poisson(10, 0)           - [Poisson].Value'     }"
				+ "{ 'this.diffTriangular  = this.diffTriangular    + triangular(2, 4, 5, 0)   - [Triangular].Value'  }"
				+ "{ 'this.diffUniform     = this.diffUniform       + uniform(2, 5, 0)         - [Uniform].Value'     }"
				+ "{ 'this.diffWeibull     = this.diffWeibull       + weibull(10, 2, 0)        - [Weibull].Value'     }"
		);

		// Start the simulation run on a new thread
		simModel.setRunListener(this);
		simModel.start();

		// Wait for the run to finish
		long timeoutMS = 5000L;
		try {
			boolean bool = countDownLatch.await(timeoutMS, TimeUnit.MILLISECONDS);
			if (!bool) {
				simModel.pause();
				String msg = String.format("Timeout at %s milliseconds, simTime=%.6f seconds. "
						+ "Model not completed.",
						timeoutMS, simModel.getSimTime());
				throw new RuntimeException(msg);
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Run interrupted: " + e.getMessage(), e);
		}
		// Test the results
		assertTrue(simModel.getSimTime() == 10.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffBeta") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffBinomial") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffDiscreteUniform") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffErlang") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffExponential") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffGamma") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffGeometric") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffLogLogistic") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffLogNormal") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffNegativeBinomial") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffNormal") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffPoisson") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffTriangular") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffUniform") == 0.0d);
		assertTrue(simModel.getDoubleValue("[Assign1].diffWeibull") == 0.0d);
	}

	@Override
	public void runEnded(SimRun run) {
		countDownLatch.countDown();
	}

	@Override
	public void handleError(Throwable t) {
		System.out.println("Error in run:");
		System.out.println(t.getMessage());
		t.printStackTrace();
		countDownLatch.countDown();
	}

}
