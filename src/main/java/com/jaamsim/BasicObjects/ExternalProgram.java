/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2021 JaamSim Software Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Parser;
import com.jaamsim.units.DimensionlessUnit;

public class ExternalProgram extends LinkedComponent {

	@Keyword(description = "External program file. "
	                     + "If the external program is written in Python, the program file "
	                     + "is the executable for Python interpreter (python.exe).",
	         exampleList = {"'c:/Program Files/program.exe'",
	                        "'c:/ProgramData/Anaconda3/python.exe'"})
	private final FileInput programFile;

	@Keyword(description = "Optional input file for the external program. "
	                     + "If the external program is written in Python, the input file "
	                     + "is the python code file (*.py)",
	         exampleList = {"'c:/test/inputs.dat'", "code.py"})
	private final FileInput inFile;

	@Keyword(description = "A list of expressions that provide the parameters to the external "
	                     + "program. The inputs must be provided in the order in which they are "
	                     + "to be entered in the external program's command line.",
	         exampleList = {"{ [Server1].Working } { '[Queue1].AverageQueueTime / 1[h]' }"})
	private final StringProvListInput dataSource;

	@Keyword(description = "The 'Value' output prior to receiving the first entity.",
	         exampleList = {"{ 0 }"})
	private final StringProvListInput initialValue;

	@Keyword(description = "Maximum time in milliseconds for the external program to finish "
	                     + "executing.",
	         exampleList = {"2000"})
	private final IntegerInput timeOut;

	private ExpResult value;  // outputs returned by the external program

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

		timeOut = new IntegerInput("TimeOut", KEY_INPUTS, 1000);
		timeOut.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(timeOut);
	}

	public ExternalProgram() {
		value = ExpCollections.wrapCollection(new ArrayList<ExpResult>(), DimensionlessUnit.class);
	}

	static final InputCallback initialValueCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ExternalProgram)ent).updateInitialValue();
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
			list.add(initialValue.getValue().get(i).getNextString(0.0d));
		}
		ArrayList<ExpResult> resList = FileToArray.getExpResultList(list, this, 0.0d);
		return ExpCollections.wrapCollection(resList, DimensionlessUnit.class);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = getInitialValue();
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = getSimTime();

		// Build the command to launch the external program
		int n = dataSource.getListSize();
		int m = 1;
		if (!inFile.isDefault())
			m = 2;
		String[] command = new String[n + m];

		// 1) Program executable
		command[0] = programFile.getValue().getPath();

		// 2) Input File (using default separator character)
		if (!inFile.isDefault()) {
			File file = new File(inFile.getValue());
			command[1] = file.getPath();
		}

		// 3) Command line parameters
		for (int i = 0; i < n; i++) {
			command[i + m] = dataSource.getValue().get(i).getNextString(simTime);
		}

		try {
			// Launch the external program
			ProcessBuilder pb = new ProcessBuilder(command);
			Process process = pb.start();

			// Wait for the program to terminate
			process.waitFor(timeOut.getValue(), TimeUnit.MILLISECONDS);

			// Check for an error in the external program
			InputStream es = process.getErrorStream();
			BufferedReader er = new BufferedReader(new InputStreamReader(es));
			String str = er.readLine();
			if (str != null) {
				StringBuilder sb = new StringBuilder(str);
				while (true) {
					String line = er.readLine();
					if (line == null)
						break;
					sb.append("\n").append(line);
				}
				er.close();
				throw new Exception(sb.toString());
			}
			er.close();

			// Collect the outputs from the program
			InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			ArrayList<String> list = new ArrayList<>();
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				Parser.tokenize(list, line, false);
			}
			reader.close();

			// Set the new output value
			ArrayList<ExpResult> resList = FileToArray.getExpResultList(list, this, simTime);
			value = ExpCollections.wrapCollection(resList, DimensionlessUnit.class);
		}
		catch (Exception e) {
			error(e.getMessage());
		}

		// Pass the entity to the next component
		sendToNextComponent(ent);
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
