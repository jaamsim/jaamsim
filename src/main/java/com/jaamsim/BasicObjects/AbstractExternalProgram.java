/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

public abstract class AbstractExternalProgram extends LinkedComponent {


	@Keyword(description = "External program file. "
	                     + "If the external program is written in Python, the program file "
	                     + "is the executable for Python interpreter (python.exe).",
	         exampleList = {"'c:/Program Files/program.exe'",
	                        "'c:/ProgramData/Anaconda3/python.exe'"})
	protected final FileInput programFile;

	@Keyword(description = "Optional input file for the external program. "
	                     + "If the external program is written in Python, the input file "
	                     + "is the python code file (*.py)",
	         exampleList = {"'c:/test/inputs.dat'", "code.py"})
	protected final FileInput inFile;

	@Keyword(description = "A list of expressions that provide the parameters to the external "
	                     + "program. The inputs must be provided in the order in which they are "
	                     + "to be entered in the external program's command line.",
	         exampleList = {"{ [Server1].Working } { '[Queue1].AverageQueueTime / 1[h]' }"})
	protected final StringProvListInput dataSource;

	@Keyword(description = "The 'Value' output prior to receiving the first entity.",
	         exampleList = {"{ 0 }"})
	protected final StringProvListInput initialValue;

	protected ExpResult value;  // outputs returned by the external program

	{
		programFile = new FileInput("ProgramFile", KEY_INPUTS, null);
		programFile.setRequired(true);
		this.addInput(programFile);

		inFile = new FileInput("InputFile", KEY_INPUTS, null);
		this.addInput(inFile);

		dataSource = new StringProvListInput("DataSource", KEY_INPUTS, null);
		this.addInput(dataSource);

		initialValue = new StringProvListInput("InitialValue", KEY_INPUTS, null);
		initialValue.setCallback(initialValueCallback);
		this.addInput(initialValue);
	}

	public AbstractExternalProgram() {
		value = ExpCollections.wrapCollection(new ArrayList<ExpResult>(), DimensionlessUnit.class);
	}

	static final InputCallback initialValueCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((AbstractExternalProgram)ent).updateInitialValue();
		}
	};

	void updateInitialValue() {
		value = getInitialValue();
	}

	protected ExpResult getInitialValue() {
		int n = 0;
		if (!initialValue.isDefault())
			n = initialValue.getListSize();

		ArrayList<String> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(initialValue.getNextString(i, this, 0.0d));
		}
		ArrayList<ExpResult> resList = FileToArray.getExpResultList(list, this, 0.0d);
		return ExpCollections.wrapCollection(resList, DimensionlessUnit.class);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = getInitialValue();
	}

	@Output(name = "Value",
	 description = "An array of values returned by the external program after parsing. "
	             + "Returned strings are converted automatically to numbers, times, or entities, "
	             + "if appropriate. "
	             + "For example, if the external program returns a single number, its value is "
	             + "'this.Value(1)'",
	    sequence = 1)
	public ExpResult getValue(double simTime) {
		return value;
	}

}
