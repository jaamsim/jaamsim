/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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
import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Parser;
import com.jaamsim.units.DimensionlessUnit;

public class ExternalProgram extends AbstractExternalProgram {

	public ExternalProgram() {}

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
			command[i + m] = dataSource.getNextString(i, simTime);
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

}
