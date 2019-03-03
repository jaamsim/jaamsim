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

import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.states.StateEntity;

public class JaamSimModel {

	private final EventManager eventManager;
	private Simulation simulation;
	private InputErrorListener inputErrorListener;
	private final AtomicLong entityCount = new AtomicLong(0);
	private final ArrayList<Entity> allInstances = new ArrayList<>(100);
	private final HashMap<String, Entity> namedEntities = new HashMap<>(100);

	public JaamSimModel() {
		Entity.setJaamSimModel(this);
		eventManager = new EventManager("DefaultEventManager");
		simulation = null;
	}

	public void setInputErrorListener(InputErrorListener l) {
		inputErrorListener = l;
	}

	public void clear() {
		eventManager.clear();
		eventManager.setTraceListener(null);
		Simulation.clear();
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
		getSimulation().start(eventManager);
	}

	/**
	 * Performs the first stage of initialization for each entity.
	 */
	public void earlyInit() {
		for (Entity each : allInstances) {
			each.earlyInit();
		}
	}

	/**
	 * Performs the second stage of initialization for each entity.
	 */
	public void lateInit() {
		for (Entity each : allInstances) {
			each.lateInit();
		}
	}

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
		endRun();
		simulation.stop();
	}

	/**
	 * Prepares the model for the next simulation run number.
	 */
	public void endRun() {
		eventManager.pause();
		eventManager.clear();

		// Destroy the entities that were generated during the run
		for (int i = 0; i < allInstances.size();) {
			Entity ent = allInstances.get(i);
			if (ent.testFlag(Entity.FLAG_GENERATED))
				ent.kill();
			else
				i++;
		}

		// Re-initialise the model
		for (Entity each : allInstances) {
			// Try/catch is required because some earlyInit methods use simTime which is only
			// available from a process thread
			try {
				each.earlyInit();
			} catch (Exception e) {}
		}

		// Initialise each entity a second time
		for (Entity each : allInstances) {
			try {
				each.lateInit();
			} catch (Exception e) {}
		}
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
