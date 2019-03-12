/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventErrorListener;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.input.InputAgent;
import com.jaamsim.states.StateEntity;
import com.jaamsim.ui.EventViewer;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;

public class JaamSimModel {

	private final EventManager eventManager;
	private Simulation simulation;
	private int runNumber;    // labels each run when multiple runs are being made
	private IntegerVector runIndexList;
	private InputErrorListener inputErrorListener;
	private final AtomicLong entityCount = new AtomicLong(0);
	private final ArrayList<Entity> allInstances = new ArrayList<>(100);
	private final HashMap<String, Entity> namedEntities = new HashMap<>(100);

	public JaamSimModel() {
		Entity.setJaamSimModel(this);
		eventManager = new EventManager("DefaultEventManager");
		simulation = null;
		runNumber = 1;
		runIndexList = new IntegerVector();
		runIndexList.add(1);
	}

	public final void setTimeListener(EventTimeListener l) {
		eventManager.setTimeListener(l);
	}

	public final void setErrorListener(EventErrorListener l) {
		eventManager.setErrorListener(l);
	}

	public void setInputErrorListener(InputErrorListener l) {
		inputErrorListener = l;
	}

	public void clear() {
		eventManager.clear();
		eventManager.setTraceListener(null);
		if (getSimulation() != null) {
			getSimulation().clear();
		}
		simulation = null;

		// Kill all entities
		while (allInstances.size() > 0) {
			Entity ent = allInstances.get(allInstances.size() - 1);
			ent.kill();
		}

		// close warning/error trace file
		InputAgent.closeLogFile();

		// Reset the run number and run indices
		runNumber = 1;
	}

	/**
	 * Pre-loads the simulation model with basic objects such as DisplayModels and Units.
	 */
	public void autoLoad() {
		InputAgent.setRecordEdits(false);
		InputAgent.readResource("<res>/inputs/autoload.cfg");
		InputAgent.setPreDefinedEntityCount( allInstances.get( allInstances.size() - 1 ).getEntityNumber());
	}

	/**
	 * Loads the specified configuration file to create the objects in the model.
	 * @param file - configuration file
	 * @throws URISyntaxException
	 */
	public void configure(File file) throws URISyntaxException {
		InputAgent.setConfigFile(file);
		InputAgent.loadConfigurationFile(file);
	}

	/**
	 * Performs consistency checks on the model inputs.
	 */
	public void validate() {
		for (Entity each : allInstances) {
			try {
				each.validate();
			}
			catch (Throwable t) {
				if (inputErrorListener != null) {
					inputErrorListener.handleInputError(t, each);
				}
				else {
					System.out.format("Validation Error - %s: %s%n", each.getName(), t.getMessage());
				}
				return;
			}
		}
	}

	/**
	 * Starts the simulation model.
	 */
	public void start() {
		validate();
		InputAgent.prepareReportDirectory();
		eventManager.clear();

		// Set up any tracing to be performed
		eventManager.setTraceListener(null);
		if (simulation.traceEvents()) {
			String evtName = InputAgent.getConfigFile().getParentFile() + File.separator + InputAgent.getRunName() + ".evt";
			EventRecorder rec = new EventRecorder(evtName);
			eventManager.setTraceListener(rec);
		}
		else if (simulation.verifyEvents()) {
			String evtName = InputAgent.getConfigFile().getParentFile() + File.separator + InputAgent.getRunName() + ".evt";
			EventTracer trc = new EventTracer(evtName);
			eventManager.setTraceListener(trc);
		}
		else if (simulation.showEventViewer()) {
			eventManager.setTraceListener(EventViewer.getInstance());
		}

		eventManager.setTickLength(simulation.getTickLength());

		runNumber = simulation.getStartingRunNumber();
		setRunIndexList();
		startRun();
	}

	/**
	 * Starts a single simulation run.
	 */
	public void startRun() {
		if (GUIFrame.getInstance() != null)
			GUIFrame.getInstance().initSpeedUp(0.0d);
		eventManager.scheduleProcessExternal(0, 0, false, new InitModelTarget(this), null);
		eventManager.resume(eventManager.secondsToNearestTick(simulation.getPauseTime()));
	}

	/**
	 * Performs the first stage of initialization for each entity.
	 */
	public void earlyInit() {
		for (Entity each : allInstances) {
			// FIXME Try/catch is required because some earlyInit methods use simTime which is only
			// available from a process thread, which is not the case when called from endRun
			try {
				each.earlyInit();
			} catch (Exception e) {}
		}
	}

	/**
	 * Performs the second stage of initialization for each entity.
	 */
	public void lateInit() {
		for (Entity each : allInstances) {
			// FIXME Try/catch is required because some lateInit methods use simTime which is only
			// available from a process thread, which is not the case when called from endRun
			try {
				each.lateInit();
			} catch (Exception e) {}
		}
	}

	public void doPauseCondition() {
		if (simulation.isPauseConditionSet())
			EventManager.scheduleUntil(pauseModelTarget, pauseCondition, null);
	}

	private final PauseModelTarget pauseModelTarget = new PauseModelTarget(this);

	private final Conditional pauseCondition = new Conditional() {
		@Override
		public boolean evaluate() {
			double simTime = EventManager.simSeconds();
			return simulation.isPauseConditionSatisfied(simTime);
		}
	};

	/**
	 * Reset the statistics for each entity.
	 */
	public void clearStatistics() {
		for (Entity ent : allInstances) {
			ent.clearStatistics();
		}

		// Reset state statistics
		for (StateEntity each : Entity.getClonesOfIterator(StateEntity.class)) {  //FIXME
			each.collectInitializationStats();
		}
	}

	/**
	 * Temporarily stops the simulation model at the present simulation time.
	 */
	public void pause() {
		eventManager.pause();
	}

	/**
	 * Re-starts the simulation model at the present simulation and allows it to proceed to the
	 * specified pause time.
	 * @param simTime - next pause time
	 */
	public void resume(double simTime) {
		eventManager.resume(eventManager.secondsToNearestTick(simTime));
	}

	/**
	 * Sets the simulation time to zero and re-initializes the model.
	 */
	public void reset() {
		eventManager.pause();
		eventManager.clear();
		killGeneratedEntities();
		earlyInit();
		lateInit();

		// Reset the run number and run indices
		runNumber = simulation.getStartingRunNumber();
		setRunIndexList();

		// Close the output reports
		InputAgent.stop();
	}

	/**
	 * Prepares the model for the next simulation run number.
	 */
	public void endRun() {

		// Execute the end of run method for each entity
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			each.doEnd();
		}

		// Print the output report
		if (simulation.getPrintReport())
			InputAgent.printReport(EventManager.simSeconds());

		// Print the selected outputs
		if (simulation.getRunOutputList().getValue() != null) {
			InputAgent.printRunOutputs(EventManager.simSeconds());
		}

		// Increment the run number and check for last run
		if (isLastRun()) {
			end();
			return;
		}

		// Start the next run
		runNumber++;
		setRunIndexList();

		eventManager.pause();
		eventManager.clear();
		killGeneratedEntities();
		earlyInit();
		lateInit();

		new Thread(new Runnable() {
			@Override
			public void run() {
				startRun();
			}
		}).start();
	}

	/**
	 * Destroys the entities that were generated during the present simulation run.
	 */
	public void killGeneratedEntities() {
		for (int i = 0; i < allInstances.size();) {
			Entity ent = allInstances.get(i);
			if (ent.testFlag(Entity.FLAG_GENERATED))
				ent.kill();
			else
				i++;
		}
	}

	/**
	 * Ends a set of simulation runs.
	 */
	public void end() {

		// Close warning/error trace file
		LogBox.logLine("Made it to do end at");
		InputAgent.closeLogFile();

		// Always terminate the run when in batch mode
		if (InputAgent.getBatch() || simulation.getExitAtStop())
			GUIFrame.shutdown(0);

		pause();
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public Simulation getSimulation() {
		if (simulation == null) {
			for (Entity ent : allInstances) {
				if (ent instanceof Simulation) {
					simulation = (Simulation) ent;
					break;
				}
			}
		}
		return simulation;
	}

	public boolean isMultipleRuns() {
		return getSimulation().getEndingRunNumber() > getSimulation().getStartingRunNumber();
	}

	public boolean isFirstRun() {
		return runNumber == getSimulation().getStartingRunNumber();
	}

	public boolean isLastRun() {
		return runNumber >= getSimulation().getEndingRunNumber();
	}

	/**
	 * Returns the run indices that correspond to a given run number.
	 * @param n - run number.
	 * @param rangeList - maximum value for each index.
	 * @return run indices.
	 */
	public static IntegerVector getRunIndexList(int n, IntegerVector rangeList) {
		IntegerVector indexList = new IntegerVector(rangeList.size());
		indexList.fillWithEntriesOf(rangeList.size(), 0);
		int denom = 1;
		for (int i=rangeList.size()-1; i>=0; i--) {
			indexList.set(i, (n-1)/denom % rangeList.get(i) + 1);
			denom *= rangeList.get(i);
		}
		return indexList;
	}

	/**
	 * Returns the run number that corresponds to a given set of run indices.
	 * @param indexList - run indices.
	 * @param rangeList - maximum value for each index.
	 * @return run number.
	 */
	public static int getRunNumber(IntegerVector indexList, IntegerVector rangeList) {
		int n = 1;
		int factor = 1;
		for (int i=indexList.size()-1; i>=0; i--) {
			n += (indexList.get(i)-1)*factor;
			factor *= rangeList.get(i);
		}
		return n;
	}

	/**
	 * Returns the input format used to specify a set of run indices.
	 * @param indexList - run indices.
	 * @return run code.
	 */
	public static String getRunCode(IntegerVector indexList) {
		StringBuilder sb = new StringBuilder();
		sb.append(indexList.get(0));
		for (int i=1; i<indexList.size(); i++) {
			sb.append("-").append(indexList.get(i));
		}
		return sb.toString();
	}

	public void setRunNumber(int n) {
		runNumber = n;
		setRunIndexList();
	}

	public void setRunIndexList() {
		runIndexList = getRunIndexList(runNumber, getSimulation().getRunIndexDefinitionList());
	}

	public int getRunNumber() {
		return runNumber;
	}

	public IntegerVector getRunIndexList() {
		return runIndexList;
	}

	public String getRunCode() {
		return getRunCode(runIndexList);
	}

	public String getRunHeader() {
		return String.format("##### RUN %s #####", getRunCode());
	}

	final long getNextEntityID() {
		return entityCount.incrementAndGet();
	}

	public final Entity getNamedEntity(String name) {
		synchronized (allInstances) {
			return namedEntities.get(name);
		}
	}

	public final long getEntitySequence() {
		long seq = (long)allInstances.size() << 32;
		seq += entityCount.get();
		return seq;
	}

	private final int idToIndex(long id) {
		int lowIdx = 0;
		int highIdx = allInstances.size() - 1;

		while (lowIdx <= highIdx) {
			int testIdx = (lowIdx + highIdx) >>> 1; // Avoid sign extension
			long testNum = allInstances.get(testIdx).getEntityNumber();

			if (testNum < id) {
				lowIdx = testIdx + 1;
				continue;
			}

			if (testNum > id) {
				highIdx = testIdx - 1;
				continue;
			}

			return testIdx;
		}

		// Entity number not found
		return -(lowIdx + 1);
	}

	public final Entity idToEntity(long id) {
		synchronized (allInstances) {
			int idx = this.idToIndex(id);
			if (idx < 0)
				return null;

			return allInstances.get(idx);
		}
	}

	public final ArrayList<? extends Entity> getEntities() {
		synchronized(allInstances) {
			return allInstances;
		}
	}

	final void renameEntity(Entity e, String newName) {
		synchronized (allInstances) {
			// Unregistered entities do not appear in the named entity hashmap, no consistency checks needed
			if (!e.testFlag(Entity.FLAG_REGISTERED)) {
				e.entityName = newName;
				return;
			}

			if (namedEntities.get(newName) != null)
				throw new ErrorException("Entity name: %s is already in use.", newName);

			String oldName = e.entityName;
			if (oldName != null && namedEntities.remove(oldName) != e)
				throw new ErrorException("Named Entities Internal Consistency error");

			e.entityName = newName;
			namedEntities.put(newName, e);
		}
	}

	final void addInstance(Entity e) {
		synchronized(allInstances) {
			allInstances.add(e);
		}
	}

	final void restoreInstance(Entity e) {
		synchronized(allInstances) {
			int index = idToIndex(e.getEntityNumber());
			if (index >= 0) {
				throw new ErrorException("Entity already included in allInstances: %s", e);
			}
			allInstances.add(-index - 1, e);
		}
	}

	final void removeInstance(Entity e) {
		synchronized (allInstances) {
			int index = idToIndex(e.getEntityNumber());
			if (index < 0)
				return;

			if (e != allInstances.remove(index))
				throw new ErrorException("Internal Consistency Error - Entity List");

			if (e.testFlag(Entity.FLAG_REGISTERED)) {
				if (e != namedEntities.remove(e.entityName))
					throw new ErrorException("Named Entities Internal Consistency error: %s", e);
			}

			e.entityName = null;
			e.setFlag(Entity.FLAG_DEAD);
		}
	}
}
