/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListInput;

public class EntityTracer extends Entity {

@Keyword(description = "The time at which to start tracing Entities",
         example = "Trace1 StartTime { 500 h }")
private final ValueInput startTime;

@Keyword(description = "The Entities to trace",
         example = "Trace1 Entities { Ent1 Ent2 Ent3 }")
private final EntityListInput<Entity> entities;

{
	startTime = new ValueInput("StartTime", "Key Inputs", 0.0d);
	startTime.setUnitType(TimeUnit.class);
	startTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
	this.addInput(startTime);

	entities = new EntityListInput<Entity>(Entity.class, "Entities", "Key Inputs", new ArrayList<Entity>(0));
	this.addInput(entities);
}

public EntityTracer() {}

@Override
public void startUp() {
	if (entities.getValue().isEmpty() || startTime.getValue() == 0.0d)
		return;

	simWait(startTime.getValue());
	for (Entity each : entities.getValue())
		each.setTraceFlag();
}
}
