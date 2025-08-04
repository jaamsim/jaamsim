/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2025 JaamSim Software Inc.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.jaamsim.events.TestFrameworkHelpers;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.ui.ExampleBox;

public class TestSimulation {

	JaamSimModel simModel;

	@Test
	public void testAllDefineableTypes() {
		JaamSimModel simModel = new JaamSimModel();
		simModel.autoLoad();

		// Define an instance of every drag-and-drop type
		for (ObjectType each: simModel.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = Input.parseEntityType(simModel, each.getName());
			if (klass == Simulation.class)
				continue;
			Entity ent = InputAgent.defineEntityWithUniqueName(simModel, klass, each.getName(), "-", true);
			assertTrue(ent != null);
		}
	}

	@Test
	public void testAllEditableInputs() {
		JaamSimModel simModel = new JaamSimModel();
		simModel.autoLoad();

		int numErrors = 0;
		// Define an instance of every drag-and-drop type
		for (ObjectType each: simModel.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = Input.parseEntityType(simModel, each.getName());
			Entity ent = null;
			if (klass == Simulation.class)
				ent = simModel.getSimulation();
			else
				ent = InputAgent.defineEntityWithUniqueName(simModel, klass, each.getName(), "-", true);
			KeywordIndex kw = new KeywordIndex("none", new ArrayList<String>(0), null);
			for (Input<?> inp : ent.getEditableInputs()) {
				// This is a hack to make the in non-default so we hit updateForInput()
				inp.setTokens(kw);
				InputAgent.apply(ent, inp, kw);
			}

			for (ValueHandle out : ent.getAllOutputs()) {
				try {
					InputAgent.getValueAsString(simModel, out, 0.0d, "%s", 1.0d, "");
				}
				catch (Throwable t) {
					System.out.println("Ent: " + ent.getName() + " Out: " + out.getName());
					numErrors++;
				}
			}
		}

		if (numErrors > 0)
			Assert.fail();
	}

	@Test
	public void testAllOutputs() {
		JaamSimModel simModel = new JaamSimModel();
		simModel.autoLoad();

		int numErrors = 0;

		// Define an instance of every drag-and-drop type
		for (ObjectType each: simModel.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = Input.parseEntityType(simModel, each.getName());
			Entity ent = null;
			if (klass == Simulation.class)
				ent = simModel.getSimulation();
			else
				ent = InputAgent.defineEntityWithUniqueName(simModel, klass, each.getName(), "-", true);

			// Check that the outputs generated for inputs do not collide with the named outputs
			HashMap<String, ValueHandle> handleMap = new HashMap<>();
			for (ValueHandle handle : OutputHandle.getAllOutputHandles(ent)) {
				handleMap.put(handle.getName(), handle);
			}
			for (Input<?> in : ent.getEditableInputs()) {
				if (!in.isOutput())
					continue;
				String name = in.getKeyword();
				if (handleMap.get(name) != null) {
					numErrors++;
					System.out.format("Name collision with output - objectType=%s, input=%s%n",
							each, name);
				}
			}
		}

		if (numErrors > 0)
			Assert.fail();
	}

	@Test
	public void testSimpleInputFile() {
		JaamSimModel simModel = new JaamSimModel();
		simModel.autoLoad();

		URL url = TestSimulation.class.getResource("Test0001.cfg");
		InputAgent.readResource(simModel, url.toString());

		simModel.getEventManager().scheduleProcessExternal(0, 0, false, new InitModelTarget(simModel), null);
		TestFrameworkHelpers.runEventsToTick(simModel.getEventManager(), Long.MAX_VALUE, 1000);
	}

	@Test
	public void testAPI() {
		JaamSimModel simModel = new JaamSimModel();
		simModel.autoLoad();

		// Definitions
		simModel.defineEntity("SimEntity", "Proto");
		simModel.defineEntity("EntityGenerator", "Gen");
		simModel.defineEntity("EntitySink", "Sink");

		// Inputs
		simModel.setInput("Gen", "PrototypeEntity", "Proto");
		simModel.setInput("Gen", "NextComponent", "Sink");
		simModel.setInput("Gen", "InterArrivalTime", "2 s");
		simModel.setInput("Simulation", "RunDuration", "9 s");

		// Perform the simulation run
		WaitForPauseListener listener = new WaitForPauseListener(simModel);
		simModel.setRunListener(listener);
		simModel.start();
		listener.waitForPause(1000L);

		// Test the results
		assertTrue(simModel.getSimTime() == 9.0d);
		assertTrue(simModel.getDoubleValue("[Gen].NumberGenerated") == 5.0d);
		assertTrue(simModel.getDoubleValue("[Sink].NumberAdded") == 5.0d);
	}

	@Test
	public void testSimultaneousRuns() {

		// Model #1
		JaamSimModel simModel = new JaamSimModel("Model1");
		simModel.autoLoad();

		simModel.defineEntity("SimEntity", "Proto");
		simModel.defineEntity("EntityGenerator", "Gen");
		simModel.defineEntity("EntitySink", "Sink");

		simModel.setInput("Gen", "PrototypeEntity", "Proto");
		simModel.setInput("Gen", "NextComponent", "Sink");
		simModel.setInput("Gen", "InterArrivalTime", "2 s");
		simModel.setInput("Simulation", "RunDuration", "1000 s");

		// Model #2
		JaamSimModel simModel2 = new JaamSimModel("Model2");
		simModel2.autoLoad();

		simModel2.defineEntity("SimEntity", "Proto");
		simModel2.defineEntity("EntityGenerator", "Gen");
		simModel2.defineEntity("EntitySink", "Sink");

		simModel2.setInput("Gen", "PrototypeEntity", "Proto");
		simModel2.setInput("Gen", "NextComponent", "Sink");
		simModel2.setInput("Gen", "InterArrivalTime", "2 s");
		simModel2.setInput("Simulation", "RunDuration", "1000 s");

		WaitForPauseListener listener = new WaitForPauseListener(simModel);
		simModel.setRunListener(listener);

		WaitForPauseListener listener2 = new WaitForPauseListener(simModel2);
		simModel2.setRunListener(listener2);

		// Start both runs
		simModel.start();
		simModel2.start();

		// Wait for both runs to finish
		listener.waitForPause(1000L);
		listener2.waitForPause(1000L);

		// Test the results
		assertTrue(simModel.getSimTime() == 1000.0d);
		assertTrue(simModel.getDoubleValue("[Gen].NumberGenerated") == 500.0d);
		assertTrue(simModel.getDoubleValue("[Sink].NumberAdded") == 500.0d);

		assertTrue(simModel2.getSimTime() == 1000.0d);
		assertTrue(simModel2.getDoubleValue("[Gen].NumberGenerated") == 500.0d);
		assertTrue(simModel2.getDoubleValue("[Sink].NumberAdded") == 500.0d);
	}

	@Test
	public void testExampleModels() {
		System.out.println();
		System.out.println("Example Models:");

		// Loop through the configuration files in the examples folder
		for (String name : ExampleBox.getExampleList()) {
			System.out.println();
			System.out.println(name);

			// Load the example model
			JaamSimModel simModel = new JaamSimModel(name + ".cfg");
			simModel.autoLoad();
			InputAgent.readResource(simModel, "<res>/examples/" + name + ".cfg");
			simModel.postLoad();
			if (simModel.getSimulation().getRunDuration() > 1000.0d)
				simModel.setInput("Simulation", "RunDuration", "1000 s");
			simModel.setInput("Simulation", "InitializationDuration", "0 s");
			simModel.setInput("Simulation", "PauseTime", "");

			// Ensure that the PrintReport input is FALSE
			assertTrue(!simModel.getSimulation().getPrintReport());

			// Validate the inputs
			boolean bool = simModel.validate();
			if (!bool)
				Assert.fail("validation failed");

			// Run the model for one hour
			long nanos = System.nanoTime();
			WaitForPauseListener listener = new WaitForPauseListener(simModel);
			simModel.setRunListener(listener);
			simModel.start();
			listener.waitForPause(5000L);
			nanos = System.nanoTime() - nanos;
			System.out.format("completed at simTime=%s, millis=%s%n", simModel.getSimTime(), nanos/1000000L);
		}
		System.out.println();
	}

	static class WaitForPauseListener implements RunListener {
		private final JaamSimModel simModel;
		private final CountDownLatch countDownLatch;

		public WaitForPauseListener(JaamSimModel mod) {
			simModel = mod;
			countDownLatch = new CountDownLatch(1);
		}

		@Override
		public void runEnded() {
			countDownLatch.countDown();
		}

		/**
		 * Delays the current thread until the simulation model is paused.
		 * @param timeoutMS - maximum time to wait in milliseconds
		 */
		public void waitForPause(long timeoutMS) {
			//System.out.format("%s.waitForPause(%s)%n", simModel, timeOut);
			try {
				boolean bool = countDownLatch.await(timeoutMS, TimeUnit.MILLISECONDS);
				if (!bool) {
					simModel.pause();
					String msg = String.format("%s - Timeout at %s milliseconds, "
							+ "simTime=%.6f seconds. Model not completed.",
							simModel, timeoutMS, simModel.getSimTime());
					throw new RuntimeException(msg);
				}
				//System.out.format("%s.waitForPause - finished%n", simModel);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void handleError(Throwable t) {}
	}
}
