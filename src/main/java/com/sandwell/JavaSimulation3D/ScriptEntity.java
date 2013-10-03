/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation3D;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.FileInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vector;

public class ScriptEntity extends Entity {


	@Keyword(description = "The name of the script file for the script entity.",
	         example = "ScriptEntity Script { test.scr }")
	private final FileInput scriptFileName;

	@Keyword(description = "The Time keyword appears inside the script file. The value represents the simulation " +
	                "time at which the next set of commands in the script are implemented.",
	         example = "ScriptEntity Time { 24.0 h }")
	private final DoubleInput scriptTime; // the time that has been read in the script

	{
		scriptFileName = new FileInput( "Script", "Key Inputs", null );
		this.addInput( scriptFileName, true );

		scriptTime = new DoubleInput( "Time", "Key Inputs", 0.0d );
		scriptTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		scriptTime.setUnits( "h" );
		this.addInput( scriptTime, false );
	}

	public ScriptEntity() {
	}


	private static class ScriptTarget extends ProcessTarget {
		final ScriptEntity script;

		ScriptTarget(ScriptEntity script) {
			this.script = script;
		}

		@Override
		public String getDescription() {
			return script.getInputName() + ".doScript";
		}

		@Override
		public void process() {
			script.doScript();
		}
	}

	@Override
	public void startUp() {
		Process.start(new ScriptTarget(this));
	}

	/**
	 * Read the script
	 */
	public void doScript() {

		// If there is no script file, do nothing
		if( scriptFileName.getValue() == null ) {
			return;
		}

		// If the script file exists, open it
		FileEntity scriptFile;
		if( FileEntity.fileExists( Util.getAbsoluteFilePath( scriptFileName.getValue() ) ) ) {
			scriptFile = new FileEntity( Util.getAbsoluteFilePath( scriptFileName.getValue() ), FileEntity.FILE_READ, true );
		}
		else {
			throw new InputErrorException( "The script file " + scriptFileName.getValue() + " was not found" );
		}

		// Read the next record
		Vector record = scriptFile.readAndParseRecord();
		if( record.size() > 0 ) {
			while( ((String)record.get( 0 )).startsWith( "\"" ) ) {
				record = scriptFile.readAndParseRecord();
			}
			Util.discardComment( record );
		}

		// While end of file has not been reached
		while( record.size() > 0 ) {

			// Process the record
			InputAgent.processData( record );

			// If a "Time" record was read, then wait until the time
			if( Tester.greaterCheckTimeStep( scriptTime.getValue(), getCurrentTime() ) ) {
				scheduleWait( scriptTime.getValue() - getCurrentTime() );
			}

			// Read the next record
			record = scriptFile.readAndParseRecord();
			while( record.size() > 0 && ((String)record.get( 0 )).startsWith( "\"" ) ) {
				record = scriptFile.readAndParseRecord();
			}
			Util.discardComment( record );
		}
	}
}
