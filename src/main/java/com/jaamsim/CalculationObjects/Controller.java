/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2024 JaamSim Software Inc.
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
package com.jaamsim.CalculationObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Generates update signals that are sent to the objects managed by the Controller.
 * @author Harry King
 *
 */
public class Controller extends DisplayEntity implements SubjectEntity {

	@Keyword(description = "Simulation time for the first update signal.",
	         exampleList = {"5 s"})
	private final SampleInput firstTime;

	@Keyword(description = "Time interval between update signals.",
	         exampleList = {"100 ms"})
	private final SampleInput interval;

	@Keyword(description = "Maximum number of updates to perform.",
	         exampleList = {"5"})
	private final SampleInput maxUpdates;

	private final ArrayList<Controllable> entityList;  // Entities controlled by this Controller.
	private int count;  // Number of update cycle completed.

	private final ProcessTarget doUpdate = new DoUpdateTarget(this);

	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	{
		firstTime = new SampleInput("FirstTime", KEY_INPUTS, 0.0d);
		firstTime.setUnitType(TimeUnit.class);
		firstTime.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(firstTime);

		interval = new SampleInput("Interval", KEY_INPUTS, Double.NaN);
		interval.setUnitType(TimeUnit.class);
		interval.setValidRange(0.0, Double.POSITIVE_INFINITY);
		interval.setRequired(true);
		this.addInput(interval);
		this.addSynonym(interval, "SamplingTime");

		maxUpdates = new SampleInput("MaxUpdates", KEY_INPUTS, Double.POSITIVE_INFINITY);
		maxUpdates.setValidRange(0, Double.POSITIVE_INFINITY);
		maxUpdates.setIntegerValue(true);
		this.addInput(maxUpdates);
	}

	public Controller() {
		entityList = new ArrayList<>();
	}

	private static final EntitySequenceSort sequenceSort = new EntitySequenceSort();
	static class EntitySequenceSort implements Comparator<Controllable> {
		@Override
		public int compare(Controllable c1, Controllable c2) {
			return Double.compare(c1.getSequenceNumber(), c2.getSequenceNumber());
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		count = 0;

		// Prepare a list of the calculation entities managed by this controller
		entityList.clear();
		for (Entity ent : getJaamSimModel().getClonesOfIterator(Entity.class, Controllable.class)) {
			Controllable con = (Controllable) ent;
			if (con.getController() == this)
				entityList.add(con);
		}

		// Sort the calculation entities into the correct sequence
		Collections.sort(entityList, sequenceSort);

		// Clear the list of observers
		subject.clear();
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		subject.registerObserver(obs);
	}

	@Override
	public void notifyObservers() {
		subject.notifyObservers();
	}

	@Override
	public ArrayList<ObserverEntity> getObserverList() {
		return subject.getObserverList();
	}

	@Override
	public void startUp() {
		super.startUp();

		// Schedule the first update
		if (getMaxUpdates(0.0d) > 0)
			EventManager.scheduleSeconds(firstTime.getNextSample(this, 0.0d), PRI_NORMAL, EVT_LIFO, doUpdate, null);
	}

	private static class DoUpdateTarget extends EntityTarget<Controller> {
		DoUpdateTarget(Controller ent) {
			super(ent, "doUpdate");
		}

		@Override
		public void process() {
			ent.doUpdate();
		}
	}

	public void doUpdate() {

		// Update the last value for each entity
		double simTime = this.getSimTime();
		for (Controllable ent : entityList) {
			ent.update(simTime);
		}

		// Notify any observers
		notifyObservers();

		// Increment the number of cycles
		count++;

		// Schedule the next update
		if (count >= getMaxUpdates(simTime))
			return;
		double dur = interval.getNextSample(this, simTime);
		EventManager.scheduleSeconds(dur, PRI_NORMAL, EVT_LIFO, doUpdate, null);
	}

	public int getCount() {
		return count;
	}

	public int getMaxUpdates(double simTime) {
		return (int) maxUpdates.getNextSample(this, simTime);
	}

	@Output(name = "EntityList",
	 description = "Objects that receive update signals from this Controller, listed in the "
	             + "sequence in which the updates are performed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public ArrayList<Controllable> getEntityList(double simTime) {
		return entityList;
	}

	@Output(name = "Count",
	 description = "Total number of updates that have been performed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public double getCount(double simTime) {
		return count;
	}

}
