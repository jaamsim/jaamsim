package com.jaamsim.BasicObjects;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.JSON.JSONConverter;
import com.jaamsim.JSON.JSONParser;
import com.jaamsim.JSON.JSONWriter;
import com.jaamsim.JSON.JSONValue;
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
import com.jaamsim.input.StringInput;
import com.jaamsim.ui.LogBox;
import com.jaamsim.units.DimensionlessUnit;

public class ExternalProgramServer extends LinkedComponent {


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

	@Keyword(description = "Name of the RPC method to call in the external program",
	                     exampleList = {"calculateDelay"})
	private final StringInput methodInput;

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
	private Process process;
	private int nextID = 1;

	private BufferedReader resReader;
	private BufferedWriter reqWriter;
	private Thread errorThread;

	{
		programFile = new FileInput("ProgramFile", KEY_INPUTS, null);
		programFile.setRequired(true);
		this.addInput(programFile);

		inFile = new FileInput("InputFile", KEY_INPUTS, null);
		this.addInput(inFile);

		methodInput = new StringInput("MethodName", KEY_INPUTS, "method");
		methodInput.setRequired(true);
		this.addInput(methodInput);

		dataSource = new StringProvListInput("DataSource", KEY_INPUTS, null);
		this.addInput(dataSource);

		initialValue = new StringProvListInput("InitialValue", KEY_INPUTS, null);
		initialValue.setCallback(initialValueCallback);
		this.addInput(initialValue);

		timeOut = new IntegerInput("TimeOut", KEY_INPUTS, 1000);
		timeOut.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(timeOut);
	}

	public ExternalProgramServer() {
		value = ExpCollections.wrapCollection(new ArrayList<ExpResult>(), DimensionlessUnit.class);
	}

	static final InputCallback initialValueCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ExternalProgramServer)ent).updateInitialValue();
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
			list.add(initialValue.getNextString(i, 0.0d));
		}
		ArrayList<ExpResult> resList = FileToArray.getExpResultList(list, this, 0.0d);
		return ExpCollections.wrapCollection(resList, DimensionlessUnit.class);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = getInitialValue();

		startProcess();
	}

	@Override
	public void kill() {
		super.kill();

		killProcess();
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
				LogBox.format("Could not start %s error monitor thread: %s", entityName, e.getMessage());
			}
		}

		@Override
		public void run() {
			if (errorReader == null) return;

			while(true) {
				try {
					String line = errorReader.readLine();
					if (line == null) break;
					LogBox.format("%s error: %s", entityName, line);
				} catch (Exception e) {
					// Some kind of logic here
					LogBox.format("Error in %s error monitor: %s", entityName, e.getMessage());
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
		double simTime = getSimTime();

		// Build the command to launch the external program
		int n = dataSource.getListSize();

		String[] args = new String[n];

		for (int i = 0; i < n; i++) {
			args[i] = dataSource.getNextString(i, simTime);
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
