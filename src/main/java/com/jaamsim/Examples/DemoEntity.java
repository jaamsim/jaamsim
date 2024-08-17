/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
package com.jaamsim.Examples;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Example of how to create a new simulation object in JaamSim. The demo entity travels back and
 * forth between two nodes with a specified travel time.
 */
public class DemoEntity extends DisplayEntity {

	@Keyword(description = "The time required to travel from one end of the route to the other.",
	         exampleList = { "3.0 h", "NormalDistribution1",
	                         "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput travelTime;

	@Keyword(description = "The position of the first node.",
	         exampleList = {"1.5 0 0 m"})
	protected final Vec3dInput node1;

	@Keyword(description = "The position of the second node.",
	         exampleList = {"1.5 0 0 m"})
	protected final Vec3dInput node2;

	private double lastUpdateTime; // time at which the distance was calculated
	private double distance; // relative position between node1 and node2 (node1 = 0, node2 = 1)
	private double speed; // relative speed of the entity between node1 and node2
	private long numberOfTrips; // number of times the entity has travelled between the nodes

	{
		travelTime = new SampleInput("TravelTime", KEY_INPUTS, Double.NaN);
		travelTime.setUnitType(TimeUnit.class);
		travelTime.setRequired(true);
		travelTime.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		this.addInput(travelTime);

		node1 = new Vec3dInput("Node1", KEY_INPUTS, new Vec3d(0.0, 0.0, 0.0));
		node1.setUnitType(DistanceUnit.class);
		this.addInput(node1);

		node2 = new Vec3dInput("Node2", KEY_INPUTS, new Vec3d(1.0, 0.0, 0.0));
		node2.setUnitType(DistanceUnit.class);
		this.addInput(node2);
	}

	public DemoEntity() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastUpdateTime = 0.0;
		distance = 0.0;
		speed = 0.0;
		numberOfTrips = 0;
	}

	@Override
	public void startUp() {
		super.startUp();
		this.startTravel();
	}

	/**
	 * Starts a new trip from one node to the other
	 */
	private void startTravel() {

		// Set the duration for the trip
		double simTime = this.getSimTime();
		double duration = travelTime.getNextSample(this, simTime);

		// Set the speed
		speed = 1.0 / duration;
		if (MathUtils.near(distance, 1.0)) {
			speed = -speed;
		}
		lastUpdateTime = simTime;

		// Schedule the time at which the entity will reach the next node
		this.scheduleProcess(duration, 5, endTravelTarget, null);
	}

	/**
	 * Ends the trip at the next node
	 */
	public void endTravel() {

		// Update the entity's relative position
		double simTime = this.getSimTime();
		distance += this.getDistanceTravelled(simTime);

		// Adjust the relative distance to avoid round-off error
		if (MathUtils.near(distance, 0.0)) {
			distance = 0.0;
		}
		if (MathUtils.near(distance, 1.0)) {
			distance = 1.0;
		}

		// Count the number of trips
		numberOfTrips++;

		// Start the next trip
		this.startTravel();
	}

	/**
	 * EndActionTarget
	 */
	private static class EndTravelTarget extends EntityTarget<DemoEntity> {
		EndTravelTarget(DemoEntity ent) {
			super(ent, "endTravel");
		}

		@Override
		public void process() {
			ent.endTravel();
		}
	}
	private final ProcessTarget endTravelTarget = new EndTravelTarget(this);

	/**
	 * Returns the relative distance travelled by the entity since the last update
	 * @param simTime - present simulation time
	 * @return relative distance travelled
	 */
	private double getDistanceTravelled(double simTime) {
		return (simTime - lastUpdateTime)*speed;
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// Leave the entity in its present position until the simulation starts
		if (simTime == 0.0)
			return;

		// Calculate the relative position of the entity at this time
		double dist = distance + this.getDistanceTravelled(simTime);

		// Set the entity's position
		Vec3d pos = new Vec3d();
		pos.interpolate3(node1.getValue(), node2.getValue(), dist);
		this.setGlobalPosition(pos);
	}

	@Output(name = "NumberOfTrips",
	 description = "Number of times the entity has travelled  in either direction between the "
	             + "two nodes.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public long getNumberOfTrips(double simTime) {
		return numberOfTrips;
	}

	@Output(name = "RelativePosition",
	 description = "Position of the entity between the two nodes: Node1 = 0, Node2 = 1.",
	    unitType = DistanceUnit.class,
	  reportable = false,
	    sequence = 2)
	public double getRelativePosition(double simTime) {
		return distance + this.getDistanceTravelled(simTime);
	}

	@Output(name = "RelativeSpeed",
	 description = "Speed of the entity expressed as the change in relative position per unit "
	             + "time.",
	    unitType = RateUnit.class,
	  reportable = false,
	    sequence = 3)
	public double getRelativeSpeed(double simTime) {
		return speed;
	}

}
