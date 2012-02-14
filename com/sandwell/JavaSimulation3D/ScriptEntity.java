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

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.FileInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vector;

public class ScriptEntity extends Entity {

	private final FileInput scriptFileName;
	private final DoubleInput scriptTime; // the time that has been read in the script

	{
		scriptFileName = new FileInput( "Script", "Script", null );
		this.addInput( scriptFileName, true );

		scriptTime = new DoubleInput( "Time", "Script", 0.0d );
		scriptTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		scriptTime.setUnits( "h" );
		this.addInput( scriptTime, false );
	}

	public ScriptEntity() {
	}

	/**
	 * This method updates the Entity for changes in the given input
	 */
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if( in == scriptTime ) {
			Tester.checkValueGreaterOrEqual( scriptTime.getValue(), getCurrentTime() );
			return;
		}
	}

	public void startUp() {
		this.startProcess( "doScript" );
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
		if( FileEntity.fileExists( scriptFileName.getValue() ) ) {
			scriptFile = new FileEntity( scriptFileName.getValue(), FileEntity.FILE_READ, true );
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
