/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2012 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.FileEntity;

public class QueueRecorder extends DisplayEntity {

	@Keyword(description = "A list of queues for which the recorder prints times " +
	                "at which objects are added and removed to the *.que file.",
			 example = "QRec QueueList { Queue1 Queue2 }")
	private final EntityListInput<Queue> queueList;

	private FileEntity outputFile; // the output file for the queue recorder

	{
		queueList = new EntityListInput<Queue>( Queue.class, "QueueList", "Key Inputs", null );
		this.addInput( queueList );
	}

	public QueueRecorder() {
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		if( getQueueList().size() == 0 )
			return;

		// Set up the output file
		String outputFileName = InputAgent.getReportFileName(String.format("%s-%s.%s",
		                                                     InputAgent.getRunName(), getName(), "que"));

		outputFile = new FileEntity( outputFileName);
		this.printOutputFileHeader();
		outputFile.flush();
	}

	public ArrayList<Queue> getQueueList() {
		return queueList.getValue();
	}

	/**
	 * Prints the header for the output file
	 */
	public void printOutputFileHeader() {
		outputFile.format( "%s Output File\n\n", this.getName() );
		outputFile.format( "Time (h)\tQueue\tObject\tAction\n" );
	}

	/**
	 * Record the addition of the given object to the given queue
	 */
	public void add( DisplayEntity ent, Queue queue ) {
		outputFile.format( "%.3f\t%s\t%s\tAdd\n", getCurrentTime(), queue.getInputName(), ent.getName() );
		outputFile.flush();
	}

	/**
	 * Record the removal of the given object from the given queue
	 */
	public void remove( DisplayEntity ent, Queue queue ) {
		outputFile.format( "%.3f\t%s\t%s\tRemove\n", getCurrentTime(), queue.getInputName(), ent.getName() );
		outputFile.flush();
	}
}
