/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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

import com.jaamsim.Graphics.Region;
import com.jaamsim.ProcessFlow.Queue;
import com.jaamsim.ProcessFlow.Server;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Thresholds.ExpressionThreshold;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class ServerAndQueue extends CompoundEntity {

	@Keyword(description = "The queue length at which the Threshold output closes.",
	         exampleList = { "3", "[InputValue1].Value" })
	private SampleInput maxQueueLength;

	{
		maxQueueLength = new SampleInput("MaxQueueLength", KEY_INPUTS, Double.POSITIVE_INFINITY);
		maxQueueLength.setUnitType(DimensionlessUnit.class);
		maxQueueLength.setIntegerValue(true);
		maxQueueLength.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(maxQueueLength);
	}

	public ServerAndQueue() {}

	@Override
	public void postDefine() {
		super.postDefine();

		// Create the sub-model components
		JaamSimModel simModel = getJaamSimModel();
		SubModelStart start = InputAgent.generateEntityWithName(simModel, SubModelStart.class, null, "Start", this, true, true);
		Queue queue = InputAgent.generateEntityWithName(simModel, Queue.class, null, "Queue", this, true, true);
		ExpressionThreshold threshold = InputAgent.generateEntityWithName(simModel, ExpressionThreshold.class, null, "Threshold", this, true, true);
		Server server = InputAgent.generateEntityWithName(simModel, Server.class, null, "Server", this, true, true);
		SubModelEnd end = InputAgent.generateEntityWithName(simModel, SubModelEnd.class, null, "End", this, true, true);

		// SubModelStart inputs
		InputAgent.applyArgs(start, "NextComponent", queue.getName());
		start.getInput("NextComponent").setLocked(true);

		// Server inputs
		InputAgent.applyArgs(server, "WaitQueue", queue.getName());
		InputAgent.applyArgs(server, "NextComponent", end.getName());
		server.getInput("WaitQueue").setLocked(true);
		server.getInput("NextComponent").setLocked(true);

		// Threshold inputs
		String expString = "sub.[Queue].QueueLength < sub.MaxQueueLength";
		InputAgent.applyArgs(threshold, "OpenCondition", expString);
		InputAgent.applyArgs(threshold, "WatchList", queue.getName());
		threshold.getInput("OpenCondition").setLocked(true);
		threshold.getInput("WatchList").setLocked(true);

		// Set the scale, size, and position of the sub-model region
		Region region = getSubModelRegion();
		InputAgent.applyValue(region, "Scale",    0.5d, "");
		InputAgent.applyVec3d(region, "Size",     new Vec3d(1.5d,  1.0d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(region, "Position", new Vec3d(0.0d, -1.5d, 0.0d), DistanceUnit.class);

		// Set the region
		InputAgent.applyArgs(start,     "Region", region.getName());
		InputAgent.applyArgs(queue,     "Region", region.getName());
		InputAgent.applyArgs(threshold, "Region", region.getName());
		InputAgent.applyArgs(server,    "Region", region.getName());
		InputAgent.applyArgs(end,       "Region", region.getName());
		start.getInput("Region").setLocked(true);
		queue.getInput("Region").setLocked(true);
		threshold.getInput("Region").setLocked(true);
		server.getInput("Region").setLocked(true);
		end.getInput("Region").setLocked(true);

		// Set the component positions within the sub-model region
		InputAgent.applyVec3d(start,     "Position", new Vec3d(-1.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(queue,     "Position", new Vec3d(-0.5d,  0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(threshold, "Position", new Vec3d( 0.5d,  0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(server,    "Position", new Vec3d( 0.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(end,       "Position", new Vec3d( 1.0d, -0.4d, 0.0d), DistanceUnit.class);
	}

	@Output(name = "MaxQueueLength",
	 description = "The queue length at which the Threshold output closes.",
	    sequence = 0)
	public int getMaxQueueLength(double simTime) {
		return (int) maxQueueLength.getNextSample(this, simTime);
	}

}
