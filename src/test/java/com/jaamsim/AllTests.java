/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	com.jaamsim.math.TestAABB.class,
	com.jaamsim.math.TestQuaternion.class,
	com.jaamsim.math.TestTransform.class,
	com.jaamsim.math.TestPlane.class,
	com.jaamsim.math.TestSphere.class,
	com.jaamsim.math.TestConvex.class,
	com.jaamsim.math.TestRay.class,
	com.jaamsim.math.TestVec2d.class,
	com.jaamsim.math.TestVec3d.class,
	com.jaamsim.math.TestVec4d.class,
	com.jaamsim.math.TestMat4d.class,
	com.jaamsim.video.vp8.TestBoolEncoder.class,
	com.jaamsim.video.vp8.TestTransforms.class,
	com.jaamsim.video.vp8.TestYUV.class,
	com.jaamsim.input.TestKeyedVec3dCurve.class,
	com.jaamsim.input.TestParser.class,
	com.jaamsim.input.TestOutput.class,
	com.jaamsim.probability.TestContinuousDistribution.class,
	com.jaamsim.probability.TestDiscreteDistribution.class,
	com.jaamsim.probability.TestErlangDistribution.class,
	com.jaamsim.probability.TestExponentialDistribution.class,
	com.jaamsim.probability.TestGammaDistribution.class,
	com.jaamsim.probability.TestLogLogisticDistribution.class,
	com.jaamsim.probability.TestLogNormalDistribution.class,
	com.jaamsim.probability.TestNormalDistribution.class,
	com.jaamsim.probability.TestTriangularDistribution.class,
	com.jaamsim.probability.TestUniformDistribution.class,
	com.jaamsim.probability.TestWeibullDistribution.class,
	com.jaamsim.probability.TestEntitlementSelector.class,
	com.jaamsim.probability.TestRandomSelector.class,
})
public class AllTests {}
