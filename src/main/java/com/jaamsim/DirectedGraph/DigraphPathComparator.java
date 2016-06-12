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
package com.jaamsim.DirectedGraph;

import java.util.Comparator;

public class DigraphPathComparator implements Comparator<DigraphPath> {

	@Override
	public int compare(DigraphPath path1, DigraphPath path2) {

		DigraphVertex vert1 = path1.getLastVertex();
		DigraphVertex vert2 = path2.getLastVertex();

		// For paths with the same sink, sort in order of smallest weight
		if (vert1 == vert2) {
			return Double.compare(path1.getWeight(), path2.getWeight());
		}

		// Otherwise sort alphabetically by sink name
		return vert1.toString().compareTo(vert2.toString());
	}

}
