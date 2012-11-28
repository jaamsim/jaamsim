/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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

import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Vector;

public class Queue extends DisplayEntity {

	@Keyword(desc = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         example = "Queue1 Spacing { 1 }")
	private final DoubleInput spacingInput;

	@Keyword(desc = "If TRUE, the program prints a queue log report (.que) consisting of lines summarizing " +
	                "the contents of the queue every time an object is added to or removed from the queue.",
	        example = "Queue1 PrintReport { TRUE }")
	private final BooleanInput printReportInput;

	protected Vector itemList;
	protected Vector3d end;
	protected int maxPerLine; // maximum items per sub line-up of queue

//	Report
	protected String reportFileName; //  File name of the report
	protected FileEntity reportFile;

//	Statistics
	protected double minElements;
	protected int maxElements;
	protected double avgElements;
	protected ArrayList<QueueRecorder> recorderList;

	{
		spacingInput = new DoubleInput("Spacing", "Key Inputs", 0.0d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacingInput, true);

		printReportInput = new BooleanInput("PrintReport", "Key Inputs", false);
		this.addInput(printReportInput, true);
	}

	public Queue() {
		itemList = new Vector( 1, 1 );
		minElements = 1E10;
		maxElements = 0;
		avgElements = 0.0;
		end = null;
		maxPerLine = 0;

		//Report
		reportFileName = "";
		reportFile = null;
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		recorderList = new ArrayList<QueueRecorder>();
		for( QueueRecorder rec : Simulation.getClonesOf( QueueRecorder.class ) ) {
			if( rec.getQueueList().contains( this ) ) {
				recorderList.add( rec );
			}
		}
	}
	// ******************************************************************************************************
	// INFO BOX METHODS
	// ******************************************************************************************************

	public Vector getInfo() {

		Vector info = super.getInfo();
		info.addElement( "Count" + "\t" + this.getCount() );
		return info;
	}

	// *******************************************************************************************************
	// INITIALIZATION
	// *******************************************************************************************************

	/**
	 * Initialize the queue
	 */
	public void initialize() {
		int doLoop = itemList.size();
		for( int x = 0; x < doLoop; x++ ) {
			DisplayEntity each = (DisplayEntity)itemList.get( x );
			each.exitRegion();
		}
		itemList.clear();

		//  Initialize reports
		this.closeReports();

		//  Report Variables
		reportFileName = InputAgent.getReportDirectory() + InputAgent.getRunName() + "-" + this.getName() + ".que";
	}

	// ******************************************************************************************************
	// QUEUE HANDLING METHODS
	// ******************************************************************************************************

	/**
     * Returns the position for a new entity at the end of the queue.
     */
    public Vector3d getEndVector3dFor( DisplayEntity perf ) {

		double locX;
		double locY;
		Vector3d endPosition;

		if( end == null ) {
			end = this.getPositionForAlignment(new Vector3d());
			end.x -= this.getSize().x / 2.0d;
			end.x += this.getSpacing();
		}

		Vector3d cent = this.getPositionForAlignment(new Vector3d());
		locX = end.x - this.getSpacing() - perf.getSize().x/2;
		locY = end.y;

		endPosition = new Vector3d( end );
		endPosition.x = cent.x + Math.cos( this.getOrientation().z ) * ( locX - cent.x ) - Math.sin( this.getOrientation().z ) * ( locY - cent.y );
		endPosition.y = cent.y + Math.cos( this.getOrientation().z ) * ( locY - cent.y ) + Math.sin( this.getOrientation().z ) * ( locX - cent.x );

		return endPosition;
    }

	/**
	 * Add an entity to the queue
	 */
	public void addLast( DisplayEntity perf ) {
		itemList.addElement( perf );
		updateGraphics();

		for( QueueRecorder rec : recorderList ) {
			rec.add( perf, this );
		}
	}

	/**
	 * Inserts the specified element at the specified position in this Queue.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 */
	public void add( int i, DisplayEntity perf ) {
		itemList.add( i, perf );
		updateGraphics();

		for( QueueRecorder rec : recorderList ) {
			rec.add( perf, this );
		}
	}

	/**
	 * Removes the specified entity from the queue
	 */
	public void remove( DisplayEntity perf ) {
		if( itemList.contains( perf ) ) {
			itemList.remove( perf );
			this.updateGraphics();

			for( QueueRecorder rec : recorderList ) {
				rec.remove( perf, this );
			}
		}
		else {
			throw new ErrorException( "item not found in queue " );
		}
	}

    /**
     * Removes the first entity from the queue
     */
    public DisplayEntity removeFirst() {
        DisplayEntity out;

        if( itemList.size() != 0 ) {
            out = (DisplayEntity)itemList.firstElement();
            itemList.removeElementAt( 0 );
            this.updateGraphics();

			for( QueueRecorder rec : recorderList ) {
				rec.remove( out, this );
			}
            return out;
        }
        else {
            throw new ErrorException( " Attempt to remove from empty queue " );
        }
    }

    /**
     * Removes the last entity from the queue
     */
    public DisplayEntity removeLast() {
        DisplayEntity out;

        if( itemList.size() != 0 ) {
            out = (DisplayEntity)itemList.lastElement();
            itemList.removeElementAt( itemList.size() - 1 );
            this.updateGraphics();

			for( QueueRecorder rec : recorderList ) {
				rec.remove( out, this );
			}
            return out;
        }
        else {
            throw new ErrorException( " Attempt to remove from empty queue " );
        }
    }

	/**
	 * Number of entities in the queue
	 */
	public int getCount() {
		return itemList.size();
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	public void updateGraphics() {

		int max = itemList.size();
		// If set, limit the number of items per sub-lane
		if (maxPerLine > 0)
			max = maxPerLine;

		Vector3d queueCenter = this.getPositionForAlignment(new Vector3d());
		Vector3d queueOrientation = getOrientation();

		Vector3d loc = new Vector3d(queueCenter);
		loc.x -= this.getSize().x / 2.0d;
		// update item locations
		for (int x = 0; x < itemList.size(); x++) {
			DisplayEntity item = (DisplayEntity)itemList.get(x);
			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			if (x == 0) {
				Vector3d cent = new Vector3d();
				cent.x = loc.x - item.getSize().x / 2.0d;
				cent.y = loc.y;
				cent.z = item.getPositionForAlignment(new Vector3d()).z;
				item.setPosition(cent);
				item.setAlignment(new Vector3d());
			}
			else if (!loc.equals(new Vector3d(queueCenter.x + item.getSize().x / 2 + this.getSpacing(), queueCenter.y, queueCenter.z))) {
				Vector3d cent = new Vector3d();
				cent.x = loc.x - item.getSize().x / 2.0d - this.getSpacing();
				cent.y = loc.y;
				cent.z = item.getPositionForAlignment(new Vector3d()).z;
				item.setPosition(cent);
				item.setAlignment(new Vector3d());
			}
			loc.x = item.getPositionForAlignment(new Vector3d()).x - item.getSize().x / 2;
			// Rotate each transporter about center of the queue-triangular so the line-up put in the right direction
			item.polarTranslationAroundPointByAngle(queueCenter, queueOrientation.z);
			item.setAlignment(new Vector3d());
			if ((x + 1) % max == 0) {
				loc.x = queueCenter.x - this.getSize().x / 2 + this.getSpacing();
				loc.y += item.getSize().y * 2.0;
			}
		}
		end = loc;
	}

	// *******************************************************************************************************
	// MISC SET- AND GET- METHODS
	// *******************************************************************************************************

	public boolean getReportFlag() {
		return printReportInput.getValue();
	}

	public void setReportFileName( String name ) {
		reportFileName = name;
	}

	public double getSpacing() {
		return spacingInput.getValue();
	}

	public void setMaxPerLine( int max ) {
		maxPerLine = max;
	}

	public Vector getItemList() {
		return itemList;
	}

	public double getPhysicalLength() {
		double length;

		length = 0.0;
		for( int x = 0; x < itemList.size(); x++ ) {
			DisplayEntity item = (DisplayEntity)itemList.get( x );
			length += item.getSize().getX() + this.getSpacing();
		}
		return length;
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	/**
	 * Clear queue statistics
	 */
	public void clearStatistics() {
		minElements = 10E10;
		maxElements = 0;
		avgElements  = 0.0;
	}

	public void updateStatistics() {

		if( itemList.size() < minElements ) {
			minElements = itemList.size();
		}

		if( itemList.size() > maxElements ) {
			maxElements = itemList.size();
		}
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	public void printUtilizationOn( FileEntity anOut ) {

		if (isActive()) {
			anOut.putStringTabs( getInputName(), 1 );
			anOut.putStringTabs( ""+minElements, 1 );
			anOut.putStringTabs( ""+maxElements, 1 );
			anOut.putStringTabs( ""+itemList.size(), 1 );
			anOut.newLine();
		}
	}

	public void printUtilizationHeaderOn( FileEntity anOut ) {
		anOut.putStringTabs( "Name", 1 );
		anOut.putStringTabs( "Min Elements", 1 );
		anOut.putStringTabs( "Max Elements", 1 );
		anOut.putStringTabs( "Present Elements", 1 );
		anOut.newLine();
	}

	// ******************************************************************************************************
	// REPORT METHODS
	// ******************************************************************************************************


	/**
	 * Close the report file
	 */
	public void closeReports() {
		if( reportFile != null ) {
			reportFile.flush();
			reportFile.close();
		}
	}
}
