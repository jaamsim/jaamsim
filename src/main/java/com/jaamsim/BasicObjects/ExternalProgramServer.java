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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.JSON.JSONConverter;
import com.jaamsim.JSON.JSONParser;
import com.jaamsim.JSON.JSONValue;
import com.jaamsim.JSON.JSONWriter;
import com.jaamsim.basicsim.Log;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.units.DimensionlessUnit;

public class ExternalProgramServer extends AbstractExternalProgram {

	@Keyword(description = "Name of the RPC method to call in the external program",
	                     exampleList = {"calculateDelay"})
	private final StringInput methodInput;

	private Process process;
	private int nextID = 1;

	private BufferedReader resReader;
	private BufferedWriter reqWriter;
	private Thread errorThread;

	{
		methodInput = new StringInput("MethodName", KEY_INPUTS, "method");
		methodInput.setRequired(true);
		this.addInput(methodInput);
	}

	public ExternalProgramServer() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		startProcess();
	}

	@Override
	public void kill() {
		super.kill();

		if (process != null) {
			killProcess();
		}
	}

	private void startProcess() {

		if (process != null) {
			killProcess();
		}

		// Build the command to launch the external program
		int m = 1;
		if (!inFile.isDefault())
			m = 2;
		String[] command = new String[m];

		// 1) Program executable
		command[0] = programFile.getValue().getPath();

		// 2) Input File (using default separator character)
		if (!inFile.isDefault()) {
			File file = new File(inFile.getValue());
			command[1] = file.getPath();
		}

		try {
			// Launch the external program
			ProcessBuilder pb = new ProcessBuilder(command);
			process = pb.start();

			OutputStream os = process.getOutputStream();
			reqWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

			InputStream is = process.getInputStream();
			resReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			// Spawn a separate thread to read stderr of the process and forward anything to the log box
			errorThread = new Thread(new ErrorLogger(process, getName()));
			errorThread.start();
		}
		catch (Exception e) {
			error(e.getMessage());
		}
	}

	private static class ErrorLogger implements Runnable {
		private Process process;
		private BufferedReader errorReader;
		private String entityName;

		public ErrorLogger(Process p, String entName) {
			process = p;
			entityName = entName;
			InputStream errorStream = process.getErrorStream();
			try {
				errorReader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
			} catch(Exception e) {
				Log.format("Could not start %s error monitor thread: %s", entityName, e.getMessage());
			}
		}

		@Override
		public void run() {
			if (errorReader == null) return;

			while(true) {
				try {
					String line = errorReader.readLine();
					if (line == null) break;
					Log.format("%s error: %s", entityName, line);
				} catch (Exception e) {
					// Some kind of logic here
					Log.format("Error in %s error monitor: %s", entityName, e.getMessage());
				}
			}
		}
	}

	private void killProcess() {
		process.destroy();
		resReader = null;
		reqWriter = null;
		process = null;
		errorThread = null; // The thread will stop on it's own when the process terminates
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = EventManager.simSeconds();

		// Build the command to launch the external program
		int n = dataSource.getListSize();

		String[] args = new String[n];

		for (int i = 0; i < n; i++) {
			args[i] = dataSource.getNextString(i, this, simTime);
		}

		try {

			ExpResult expArgs = ExpCollections.wrapCollection(args, DimensionlessUnit.class);
			JSONValue jsonArgs = JSONConverter.fromExpResult(expArgs);

			JSONValue request = JSONValue.makeObject();
			request.mapVal.put("jsonrpc", JSONValue.makeStringVal("2.0"));
			request.mapVal.put("id", JSONValue.makeNumVal(++nextID));
			request.mapVal.put("method", JSONValue.makeStringVal(methodInput.getValue()));
			request.mapVal.put("params", jsonArgs);

			String reqJSON = JSONWriter.writeJSONValue(request);

			reqWriter.write(reqJSON);
			reqWriter.write('\n');
			reqWriter.flush();

			// Collect the outputs from the program
			JSONParser resParser = new JSONParser();
			while (true) {
				String line = resReader.readLine();
				resParser.addPiece(line);
				if (resParser.isElementComplete())
					break;
				if (line == null) {
					throw new Exception("External server program terminated early!");
				}
			}
			if (!resParser.isElementComplete()) {
				throw new Exception("External program returned invalid JSON");
			}
			JSONValue response = resParser.parse();
			// Validate the response
			if (!response.isMap() || !response.mapVal.get("jsonrpc").isString() || !response.mapVal.get("jsonrpc").stringVal.equals("2.0")) {
				throw new Exception("External server returned invalid JSON");
			}
			// Check for returned error
			JSONValue err = response.mapVal.get("error");
			if (err != null) {
				// returned error
				if (!err.isMap()) throw new Exception("External server program returned invalid error object");
				String errMsg = err.mapVal.get("message").stringVal;
				throw new Exception(String.format("External server returned error: %s", errMsg));
			}

			// Set the new output value
			JSONValue result = response.mapVal.get("result");
			if (result == null) {
				throw new Exception(String.format("JSON-RPC response missing result field"));
			}
			value = JSONConverter.toExpResult(result);

		}
		catch (Exception e) {
			error(e.getMessage());
		}

		// Pass the entity to the next component
		sendToNextComponent(ent);
	}

}
