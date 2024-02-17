/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2024 JaamSim Software Inc.
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.jaamsim.SubModels.SubModel;
import com.jaamsim.input.Input;

public class TestEntityDefinitions {

	JaamSimModel simModel;

	@Test
	public void testDefinitions() {
		simModel = new JaamSimModel();
		simModel.autoLoad();

		// Chain of sub-models:
		// Sub-model A contains component A.B, which in turn contains component A.B.C
		SubModel a = simModel.createInstance(SubModel.class, null, "A", null, true, false, true, true); // A
		SubModel ab = simModel.createInstance(SubModel.class, null, "B", a, true, false, true, true);   // A.B
		SubModel abc = simModel.createInstance(SubModel.class, null, "C", ab, true, false, true, true); // A.B.C

		assertTrue(a.getName().equals("A"));
		assertTrue(ab.getName().equals("A.B"));
		assertTrue(abc.getName().equals("A.B.C"));

		// Chain of clones:
		// Sub-model A2 is a clone of A1, which in turn is a clone of sub-model A
		SubModel a1 = simModel.createInstance(SubModel.class, a, "A1", null, true, false, true, true);  // clone of A
		SubModel a2 = simModel.createInstance(SubModel.class, a1, "A2", null, true, false, true, true); // clone of clone of A

		assertTrue(a1.getName().equals("A1"));
		assertTrue(a2.getName().equals("A2"));

		// Clones of SubModels:
		// Sub-model AB1 is a clone of A.B, sub-model ABC1 is a clone of A.B.C
		SubModel ab1 = simModel.createInstance(SubModel.class, ab, "AB1", null, true, false, true, true);     // clone of A.B
		SubModel abc1 = simModel.createInstance(SubModel.class, abc, "ABC1", null, true, false, true, true);  // clone of A.B.C

		assertTrue(ab1.getName().equals("AB1"));
		assertTrue(abc1.getName().equals("ABC1"));

		// Dependency levels
		assertTrue(a.getDependenceLevel() == 0);     // A has no dependencies
		assertTrue(ab.getDependenceLevel() == 1);    // A.B has dependencies A
		assertTrue(abc.getDependenceLevel() == 2);   // A.B.C has dependencies A.B, A

		assertTrue(a1.getDependenceLevel() == 2);    // A1 has dependencies A, A.B
		assertTrue(a2.getDependenceLevel() == 3);    // A2 has dependencies A1, A, A.B

		assertTrue(ab1.getDependenceLevel() == 3);   // AB1 has dependencies A.B, A.B.C, A
		assertTrue(abc1.getDependenceLevel() == 3);  // ABC1 has dependencies A.B.C, A.B, A

		// When the input file is saved, the entities must defined in the following sequence,
		// with ties defaulting to alphabetical order (numbers before letters):
		// A, A.B, A.B.C, A1, A2, AB1, ABC1
		List<Entity> correctList = Arrays.asList(a, ab, abc, a1, a2, ab1, abc1);
		List<Entity> list = new ArrayList<>(correctList);
		Collections.reverse(list);
		Collections.sort(list, new Comparator<Entity>() {
			@Override
			public int compare(Entity ent0, Entity ent1) {
				int ret = Integer.compare(ent0.getDependenceLevel(), ent1.getDependenceLevel());
				if (ret != 0)
					return ret;
				return Input.uiSortOrder.compare(ent0, ent1);
			}
		});
		//System.out.println(list);
		assertTrue(list.equals(correctList));
	}

}
