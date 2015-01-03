/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

public interface QueueUser {

	/**
	 * Returns a list of the Queues used by this object.
	 * @return the Queue list.
	 */
	public abstract ArrayList<Queue> getQueues();

	/**
	 * Called whenever an entity is added to one of the Queues used
	 * by this object.
	 */
	public abstract void queueChanged();

}
