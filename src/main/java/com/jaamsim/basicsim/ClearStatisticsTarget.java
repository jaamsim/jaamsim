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
package com.jaamsim.basicsim;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.states.StateEntity;

public class ClearStatisticsTarget extends ProcessTarget {

	@Override
	public String getDescription() {
		return "ClearStatistics";
	}

	@Override
	public void process() {

		// Reset the statistics for each entity in the model
		for (Entity ent : Entity.getAll()) {
			ent.clearStatistics();
		}

		// Reset state statistics
		for (StateEntity each : Entity.getClonesOfIterator(StateEntity.class)) {
			each.clearReportStats();
		}
	}

}
