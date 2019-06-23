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

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.Queue;
import com.jaamsim.ProcessFlow.Server;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.SubModels.SubModelEnd;
import com.jaamsim.SubModels.SubModelStart;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class ServerAndQueue extends CompoundEntity {

	@Keyword(description = "The service time required to process an entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private SampleInput serviceTime;

	public ServerAndQueue() {}

	@Override
	public void postDefine() {
		super.postDefine();

		// Create the sub-model components
		JaamSimModel simModel = getJaamSimModel();
		SubModelStart start = InputAgent.generateEntityWithName(simModel, SubModelStart.class, getComponentName("Start"), true, true);
		Queue queue = InputAgent.generateEntityWithName(simModel, Queue.class, getComponentName("Queue"), true, true);
		Server server = InputAgent.generateEntityWithName(simModel, Server.class, getComponentName("Server"), true, true);
		SubModelEnd end = InputAgent.generateEntityWithName(simModel, SubModelEnd.class, getComponentName("End"), true, true);

		// Add component inputs to the sub-model
		serviceTime = (SampleInput) server.getInput("ServiceTime");
		addInput(serviceTime);

		// SubModelStart inputs
		InputAgent.applyArgs(start, "NextComponent", queue.getName());

		// Server inputs
		InputAgent.applyArgs(server, "WaitQueue", queue.getName());
		InputAgent.applyArgs(server, "NextComponent", end.getName());

		// Set the component positions within the sub-model region
		InputAgent.applyVec3d(start,  "Position", new Vec3d(-1.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(queue,  "Position", new Vec3d(-0.5d,  0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(server, "Position", new Vec3d( 0.0d, -0.4d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(end,    "Position", new Vec3d( 1.0d, -0.4d, 0.0d), DistanceUnit.class);

		// Set the scale, size, and position of the sub-model region
		setDefaultRegionScale(0.5d);
		setDefaultRegionSize(new Vec3d(3.0d, 2.0d, 0.0d));
		setDefaultRegionPosition(new Vec3d(0.0d, -1.5d, 0.0d));

		// Set the component list
		ArrayList<DisplayEntity> compList = new ArrayList<>();
		compList.add(start);
		compList.add(queue);
		compList.add(server);
		compList.add(end);
		setComponentList(compList);
	}

}
