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
package com.jaamsim.ProcessFlow;

import com.jaamsim.Graphics.DisplayEntity;

public class ProcessorData {

	private long numberReceived;     // Number of entities received after initialisation
	private long numberProcessed; // Number of entities processed after initialisation
	private long initialNumberReceived;     // Number of entities received during initialisation
	private long initialNumberProcessed; // Number of entities processed during initialisation
	private DisplayEntity receivedEntity; // Entity received most recently
	private double releaseTime = Double.NaN;

	public ProcessorData() {}

	public void clear() {
		numberReceived = 0;
		numberProcessed = 0;
		initialNumberReceived = 0;
		initialNumberProcessed = 0;
		receivedEntity = null;
		releaseTime = Double.NaN;
	}

	public void clearStatistics() {
		initialNumberReceived = numberReceived;
		initialNumberProcessed = numberProcessed;
		numberReceived = 0;
		numberProcessed = 0;
	}

	public void receiveEntity(DisplayEntity ent) {
		receivedEntity = ent;
		numberReceived++;
	}

	public void releaseEntity(double simTime) {
		numberProcessed++;
		releaseTime = simTime;
	}

	public void setReceivedEntity(DisplayEntity ent) {
		receivedEntity = ent;
	}

	/**
	 * Returns the last entity that was received.
	 * @return last entity received
	 */
	public DisplayEntity getReceivedEntity() {
		return receivedEntity;
	}

	/**
	 * Returns the time at which the last entity was released.
	 * @return last release time
	 */
	public double getReleaseTime() {
		return releaseTime;
	}

	/**
	 * Returns the number of entities that have been received from upstream during the entire
	 * simulation run, including the initialisation period.
	 * @return total number of entities that were added
	 */
	public long getTotalNumberReceived() {
		return initialNumberReceived + numberReceived;
	}

	/**
	 * Returns the number of entities that have been passed downstream during the entire
	 * simulation run, including the initialisation period.
	 * @return total number of entities that were processed
	 */
	public long getTotalNumberProcessed() {
		return initialNumberProcessed + numberProcessed;
	}

	/**
	 * Returns the number of entities that have been received from upstream after the
	 * initialisation period.
	 * @return number of entities that were added
	 */
	public long getNumberReceived() {
		return numberReceived;
	}

	/**
	 * Returns the number of entities that have been passed downstream after the
	 * initialisation period.
	 * @return number of entities that were processed
	 */
	public long getNumberProcessed() {
		return numberProcessed;
	}

	/**
	 * Returns the number of entities that have been received but whose processing has not been
	 * completed yet.
	 * @return number of entities that being processed
	 */
	public long getNumberInProgress() {
		return  initialNumberReceived + numberReceived - initialNumberProcessed - numberProcessed;
	}

}
