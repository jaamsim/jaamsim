/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2018 JaamSim Software Inc.
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
package com.jaamsim.resourceObjects;

public interface ResourceUser {

	/**
	 * Returns whether the specified Resource can be seized by this object.
	 * @param res - Resource to be seized.
	 * @return true if the specified Resource can be seized.
	 */
	public abstract boolean requiresResource(ResourceProvider res);

	/**
	 * Returns whether there is an eligible entity that is waiting to seize one or more Resources.
	 * @return true if an entity is waiting
	 */
	public abstract boolean hasWaitingEntity();

	/**
	 * Returns the priority of the first eligible entity that is waiting to seize Resources.
	 * @return priority
	 */
	public abstract int getPriority();

	/**
	 * Returns the time that the first eligible entity has been waiting to seize Resources.
	 * @return wait time
	 */
	public abstract double getWaitTime();

	/**
	 * Returns whether sufficient Resources are available to start processing the next entity.
	 * @return true if the next entity can start
	 */
	public abstract boolean isReadyToStart();

	/**
	 * Seizes the Resources for the next entity and begins its processing.
	 */
	public abstract void startNextEntity();

	/**
	 * Returns whether the seize object uses a Resource that required strict-order.
	 * @return true if a strict-order Resource is required
	 */
	public abstract boolean hasStrictResource();
}
