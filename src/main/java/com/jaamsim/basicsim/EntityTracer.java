/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;

public class EntityTracer extends Entity {

@Keyword(description = "The time at which to start tracing Entities",
         exampleList = {"500 h"})
private final ValueInput startTime;

@Keyword(description = "The Entities to trace",
         exampleList = {"Ent1 Ent2 Ent3"})
private final EntityListInput<Entity> entities;

{
	startTime = new ValueInput("StartTime", KEY_INPUTS, 0.0d);
	startTime.setUnitType(TimeUnit.class);
	startTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
	this.addInput(startTime);

	entities = new EntityListInput<>(Entity.class, "Entities", KEY_INPUTS, new ArrayList<Entity>(0));
	this.addInput(entities);
}

public EntityTracer() {}

@Override
public void startUp() {
	super.startUp();

	if (entities.getValue().isEmpty() || startTime.getValue() == 0.0d)
		return;

	simWait(startTime.getValue(), 0);
	for (Entity each : entities.getValue())
		each.setTraceFlag();
}
}
