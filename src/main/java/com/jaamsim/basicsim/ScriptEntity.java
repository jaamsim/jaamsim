/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;

public class ScriptEntity extends Entity {
	@Keyword(description = "The name of the script file for the script entity.",
	         exampleList = {"test.scr"})
	private final FileInput scriptFileName;

	@Keyword(description = "The Time keyword appears inside the script file. The value represents "
	                     + "the simulation time at which the next set of commands in the script "
	                     + "are implemented.",
	         exampleList = {"24.0 h"})
	private final ValueInput scriptTime; // the time that has been read in the script

	private ArrayList<ArrayList<String>> tokens;
	private int lastTokenIdx;

	{
		scriptFileName = new FileInput( "Script", KEY_INPUTS, null );
		scriptFileName.setRequired(true);
		this.addInput( scriptFileName );

		scriptTime = new ValueInput("Time", KEY_INPUTS, 0.0d);
		scriptTime.setUnitType(TimeUnit.class);
		scriptTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(scriptTime);
	}

	public ScriptEntity() {
		tokens = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		tokens.clear();
		lastTokenIdx = -1;

		// Open the script file
		tokens = FileInput.getTokensFromURI(scriptFileName.getValue());
		boolean record = InputAgent.recordEdits();
		InputAgent.setRecordEdits(false);
		// Read records until a Time record is read
		// Restarts will work for simple scripts with a record at Time 0
		// Restarts should work for all scripts provided the script has initial inputs before the first Time record
		for (lastTokenIdx++; lastTokenIdx < tokens.size(); lastTokenIdx++) {
			InputAgent.processKeywordRecord(this.getJaamSimModel(), tokens.get(lastTokenIdx), null);
			if( tokens.get(lastTokenIdx).get( 0 ).equals( this.getName() ) ) {
				if( tokens.get( lastTokenIdx ).get( 1 ).equals( "Time" ) ) {
					lastTokenIdx--;
					InputAgent.setRecordEdits(record);
					return;
				}
			}
		}
	}

	private static class ScriptTarget extends ProcessTarget {
		final ScriptEntity script;

		ScriptTarget(ScriptEntity script) {
			this.script = script;
		}

		@Override
		public String getDescription() {
			return script.getName() + ".doScript";
		}

		@Override
		public void process() {
			script.doScript();
		}
	}
	private final ProcessTarget targ = new ScriptTarget(this);

	@Override
	public void startUp() {
		doScript();
	}

	/**
	 * Read the script
	 */
	public void doScript() {
		boolean record = InputAgent.recordEdits();
		InputAgent.setRecordEdits(false);
		for (lastTokenIdx++; lastTokenIdx < tokens.size(); lastTokenIdx++) {
			InputAgent.processKeywordRecord(this.getJaamSimModel(), tokens.get(lastTokenIdx), null);
			// If a "Time" record was read, then wait until the time
			long delayTicks = EventManager.current().secondsToNearestTick(scriptTime.getValue()) - getSimTicks();
			if (delayTicks > 0) {
				scheduleProcessTicks(delayTicks, 0, targ);
				break;
			}
		}
		InputAgent.setRecordEdits(record);
	}
}
