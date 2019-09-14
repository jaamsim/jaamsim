/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	com.jaamsim.events.TestEventManager.class,
	com.jaamsim.events.TestEventTree.class,
	com.jaamsim.events.TestSchedEvent.class,
	com.jaamsim.math.TestAABB.class,
	com.jaamsim.math.TestColor4d.class,
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
	com.jaamsim.math.TestInterners.class,
	com.jaamsim.rng.TestMRG1999a.class,
	com.jaamsim.video.vp8.TestBoolEncoder.class,
	com.jaamsim.video.vp8.TestTransforms.class,
	com.jaamsim.video.vp8.TestYUV.class,
	com.jaamsim.input.TestKeyedVec3dCurve.class,
	com.jaamsim.input.TestExpParser.class,
	com.jaamsim.input.TestParser.class,
	com.jaamsim.input.TestOutput.class,
	com.jaamsim.basicsim.TestSimulation.class,
	com.jaamsim.basicsim.TestSimCalendar.class,
	com.jaamsim.probability.TestContinuousDistribution.class,
	com.jaamsim.probability.TestDiscreteDistribution.class,
	com.jaamsim.probability.TestErlangDistribution.class,
	com.jaamsim.probability.TestExponentialDistribution.class,
	com.jaamsim.probability.TestGammaDistribution.class,
	com.jaamsim.probability.TestBetaDistribution.class,
	com.jaamsim.probability.TestLogLogisticDistribution.class,
	com.jaamsim.probability.TestLogNormalDistribution.class,
	com.jaamsim.probability.TestNormalDistribution.class,
	com.jaamsim.probability.TestTriangularDistribution.class,
	com.jaamsim.probability.TestUniformDistribution.class,
	com.jaamsim.probability.TestWeibullDistribution.class,
	com.jaamsim.probability.TestEntitlementSelector.class,
	com.jaamsim.probability.TestBooleanSelector.class,
	com.jaamsim.MeshFiles.TestVertexMap.class,
	com.jaamsim.MeshFiles.TestDataBlocks.class,
	com.jaamsim.Statistics.TestStatistics.class,
	com.jaamsim.BasicObjects.TestFileToVector.class,
	com.jaamsim.BasicObjects.TestFileToMatrix.class,
	com.jaamsim.BasicObjects.TestFileToHashMap.class,
})
public class AllTests {}
