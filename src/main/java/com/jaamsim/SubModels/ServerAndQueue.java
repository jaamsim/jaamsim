/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import com.jaamsim.ProcessFlow.Queue;
import com.jaamsim.ProcessFlow.Server;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.SubModels.SubModelEnd;
import com.jaamsim.SubModels.SubModelStart;
import com.jaamsim.Thresholds.ExpressionThreshold;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class ServerAndQueue extends CompoundEntity {

	@Keyword(description = "The service time required to process an entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private SampleInput serviceTime;

	@Keyword(description = "The queue length at which the Threshold output closes.",
	         exampleList = { "3", "[InputValue1].Value" })
	private SampleInput maxQueueLength;

	{
		SampleConstant def = new SampleConstant(Double.POSITIVE_INFINITY);
		maxQueueLength = new SampleInput("MaxQueueLength", KEY_INPUTS, def);
		maxQueueLength.setUnitType(DimensionlessUnit.class);
		maxQueueLength.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(maxQueueLength);
	}

	public ServerAndQueue() {}

	@Override
	public void postDefine() {
		super.postDefine();

		// Create the sub-model components
		JaamSimModel simModel = getJaamSimModel();
		SubModelStart start = InputAgent.generateEntityWithName(simModel, SubModelStart.class, "Start", this, true, true);
		Queue queue = InputAgent.generateEntityWithName(simModel, Queue.class, "Queue", this, true, true);
		ExpressionThreshold threshold = InputAgent.generateEntityWithName(simModel, ExpressionThreshold.class, "Threshold", this, true, true);
		Server server = InputAgent.generateEntityWithName(simModel, Server.class, "Server", this, true, true);
		SubModelEnd end = InputAgent.generateEntityWithName(simModel, SubModelEnd.class, "End", this, true, true);

		// Add component inputs to the sub-model
		serviceTime = (SampleInput) server.getInput("ServiceTime");
		addInput(serviceTime);

		// SubModelStart inputs
		InputAgent.applyArgs(start, "NextComponent", queue.getName());

		// Server inputs
		InputAgent.applyArgs(server, "WaitQueue", queue.getName());
		InputAgent.applyArgs(server, "NextComponent", end.getName());

		// Threshold inputs
		String expString = "sub.[Queue].QueueLength < sub.MaxQueueLength";
		InputAgent.applyArgs(threshold, "OpenCondition", expString);
		InputAgent.applyArgs(threshold, "WatchList", queue.getName());

		// Set the component positions within the sub-model region
		InputAgent.applyVec3d(start,     "Position", new Vec3d(-1.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(queue,     "Position", new Vec3d(-0.5d,  0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(threshold, "Position", new Vec3d( 0.5d,  0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(server,    "Position", new Vec3d( 0.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(end,       "Position", new Vec3d( 1.0d, -0.4d, 0.0d), DistanceUnit.class);

		// Set the scale, size, and position of the sub-model region
		setDefaultRegionScale(0.5d);
		setDefaultRegionSize(new Vec3d(3.0d, 2.0d, 0.0d));
		setDefaultRegionPosition(new Vec3d(0.0d, -1.5d, 0.0d));

		// Set the region
		String regionName = getSubModelRegion().getName();
		InputAgent.applyArgs(start,     "Region", regionName);
		InputAgent.applyArgs(queue,     "Region", regionName);
		InputAgent.applyArgs(threshold, "Region", regionName);
		InputAgent.applyArgs(server,    "Region", regionName);
		InputAgent.applyArgs(end,       "Region", regionName);
	}

	@Output(name = "MaxQueueLength",
	 description = "The queue length at which the Threshold output closes.",
	    sequence = 0)
	public int getMaxQueueLength(double simTime) {
		return (int) maxQueueLength.getValue().getNextSample(simTime);
	}

}
