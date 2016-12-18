/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 JaamSim Software Inc.
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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class JaamSimModel {
	final AtomicLong entityCount = new AtomicLong(0);
	final ArrayList<Entity> allInstances = new ArrayList<>(100);
	final HashMap<String, Entity> namedEntities = new HashMap<>(100);

	public JaamSimModel() {
	}

	final long getNextEntityID() {
		return entityCount.incrementAndGet();
	}
}
