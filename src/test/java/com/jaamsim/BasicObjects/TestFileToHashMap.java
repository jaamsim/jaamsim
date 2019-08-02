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
package com.jaamsim.BasicObjects;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Before;
import org.junit.Test;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.InputAgent;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class TestFileToHashMap {

	JaamSimModel simModel;

	@Before
	public void setupTests() {
		simModel = new JaamSimModel();
		simModel.createInstance(Simulation.class);
	}

	@Test
	public void testValue() {
		FileToHashMap fileToHashMap = InputAgent.defineEntityWithUniqueName(simModel, FileToHashMap.class, "FileToHashMap1", "", true);
		LinkedHashMap<String, ArrayList<Object>> data = new LinkedHashMap<>();

		ArrayList<Object> list1 = new ArrayList<>();
		list1.add(1.5d);
		list1.add(2.5d);
		data.put("Fred", list1);

		ArrayList<Object> list2 = new ArrayList<>();
		list2.add(3.5d);
		list2.add(4.5);
		data.put("George", list2);
		try {
			fileToHashMap.setValue(data);

			ExpResult val = fileToHashMap.getOutputHandle("Value").getValue(0.0d, ExpResult.class);
			//System.out.println(val);

			Class<? extends Unit> ut = DimensionlessUnit.class;
			ExpResult ind1 = ExpResult.makeNumResult(1, ut);
			ExpResult ind2 = ExpResult.makeNumResult(2, ut);

			ExpResult fred = val.colVal.index(ExpResult.makeStringResult("Fred"));
			assertTrue( fred.colVal.index(ind1).value == 1.5d );
			assertTrue( fred.colVal.index(ind2).value == 2.5d );

			ExpResult george = val.colVal.index(ExpResult.makeStringResult("George"));
			assertTrue( george.colVal.index(ind1).value == 3.5d );
			assertTrue( george.colVal.index(ind2).value == 4.5d );
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
