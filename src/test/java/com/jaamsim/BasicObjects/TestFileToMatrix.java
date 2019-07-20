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

import org.junit.Before;
import org.junit.Test;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.InputAgent;

public class TestFileToMatrix {

	JaamSimModel simModel;

	@Before
	public void setupTests() {
		simModel = new JaamSimModel();
		simModel.createInstance(Simulation.class);
	}

	@Test
	public void testValue() {
		FileToMatrix fileToMatrix = InputAgent.defineEntityWithUniqueName(simModel, FileToMatrix.class, "FileToMatrix1", "", true);
		ArrayList<ArrayList<Object>> data = new ArrayList<>();

		ArrayList<Object> list1 = new ArrayList<>();
		list1.add(1.5d);
		list1.add("abc");
		data.add(list1);

		ArrayList<Object> list2 = new ArrayList<>();
		list2.add(2.5d);
		list2.add("def");
		data.add(list2);
		try {
			fileToMatrix.setValue(data);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}

		ArrayList<ArrayList<ExpResult>> val = fileToMatrix.getOutputHandle("Value").getValue(0.0d, ArrayList.class);
		//System.out.println(val);
		assertTrue( val.get(0).get(0).value == 1.5d );
		assertTrue( val.get(0).get(1).stringVal.equals("abc") );
		assertTrue( val.get(1).get(0).value == 2.5d );
		assertTrue( val.get(1).get(1).stringVal.equals("def") );
	}

}
