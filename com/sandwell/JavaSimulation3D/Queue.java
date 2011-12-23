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

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Vector;
import javax.vecmath.Vector3d;

public class Queue extends DisplayEntity {
	private final DoubleInput spacingInput;
	private final BooleanInput printReportInput;

	protected int maxQueued;
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
	}

	/**
	 * Add an entity to the queue
	 */
	public void addLastWithoutUpdateGraphic( DisplayEntity perf ) {
		itemList.addElement( perf );
	}

	public void setMaxQueued( int max ) {
		maxQueued = max;
	}

	/**
	 * Removes the specified entity from the queue
	 */
	public void remove( DisplayEntity perf ) {
		if( itemList.contains( perf ) ) {
			itemList.remove( perf );
			this.updateGraphics();
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

            return out;
        }
        else {
            throw new ErrorException( " Attempt to remove from empty queue " );
        }
    }

    /**
     * Removes the first entity from the queue
     */
    public DisplayEntity removeFirstProgressiveUpdate( double speed ) {
    	DisplayEntity out;

    	if( itemList.size() != 0 ) {
    		out = (DisplayEntity)itemList.firstElement();
    		itemList.removeElementAt( 0 );
    		this.updateGraphicsAtSpeed( speed );

    		return out;
    	}
    	else {
    		throw new ErrorException( " Attempt to remove from empty queue " );
    	}
    }

    /**
     * Removes the first entity from the queue without updating
     */
    public DisplayEntity removeFirstProgressiveItem( double speed ) {
    	DisplayEntity out;

    	if( itemList.size() != 0 ) {
    		out = (DisplayEntity)itemList.firstElement();
    		itemList.removeElementAt( 0 );

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


	// ******************************************************************************************************
	// GRAPHICS METHODS
	// ******************************************************************************************************

	/**
	 * Update the position of all entities in the queue.
	 * ASSUME that entities will line up according to the orientation of the queue.
	 */
	public void updateGraphics() {

		int max;

		Vector3d centerOfQueue = this.getPositionForAlignment(new Vector3d());

		// get the orientation of the queue
		Vector3d queueOrientation = getOrientation();

			// determine maximum items per sub lane
			if( maxPerLine == 0 ) {
				max = itemList.size();
			}
			else{
				max = maxPerLine;
			}

			Vector3d loc = this.getPositionForAlignment(new Vector3d());
			loc.x -= this.getSize().x / 2.0d;
			// update item locations
			for( int x = 0; x < itemList.size(); x++ ) {
				DisplayEntity item = (DisplayEntity)itemList.get( x );
				//  Rotate each transporter about its center so it points to the right direction
				item.setOrientation(queueOrientation);
				if( x == 0) {
					Vector3d cent = new Vector3d();
					cent.x = loc.x - item.getSize().x / 2.0d;
					cent.y = loc.y;
					cent.z = item.getPositionForAlignment(new Vector3d()).z;
					item.setPosition(cent);
					item.setAlignment(new Vector3d());
				}
				else if( !loc.equals( new Vector3d( this.getPositionForAlignment(new Vector3d()).x + item.getSize().x/2 + this.getSpacing(), this.getPositionForAlignment(new Vector3d()).y, this.getPositionForAlignment(new Vector3d()).z ) ) ) {
					Vector3d cent = new Vector3d();
					cent.x = loc.x - item.getSize().x / 2.0d - this.getSpacing();
					cent.y = loc.y;
					cent.z = item.getPositionForAlignment(new Vector3d()).z;
					item.setPosition(cent);
					item.setAlignment(new Vector3d());
				}
				loc.x = item.getPositionForAlignment(new Vector3d()).x - item.getSize().x/2;
				//  Rotate each transporter about center of the queue-triangular so the line-up put in the right direction
				item.polarTranslationAroundPointByAngle(centerOfQueue, queueOrientation.z);
				if( (x + 1) % max == 0 ) {
					loc.x = this.getPositionForAlignment(new Vector3d()).x - this.getSize().x/2 + this.getSpacing();
					loc.y += item.getSize().y*2.0;
				}
			}
			end = loc;
	}

	/**
	 * Update the position of all entities in the queue.
	 * ASSUME that entities will line up according to the orientation of the queue.
	 */
	public void updateGraphicsAtSpeed( double speed ) {
		Double maxTime = new Double( 0.0d );
		Vector3d loc = this.getPositionForAlignment(new Vector3d());
		loc.x -= this.getSize().x / 2.0d;
		loc.x += this.getSpacing();

			end = null;
			for( int x = 0; x < itemList.size(); x++ ) {
				DisplayEntity item = (DisplayEntity)itemList.get( x );

            	Vector3d endPosition = getEndVector3dFor( item );
            	Vector3d diff = new Vector3d( item.getPositionForAlignment(new Vector3d()).getX() - endPosition.getX(), item.getPositionForAlignment(new Vector3d()).getY() - endPosition.getY(), 0.0 );
            	Double time = new Double( Math.hypot( diff.getX(), diff.getY() ) / speed );
            	if(maxTime.compareTo( time ) < 0 ) {
            		maxTime = time;
            	}
            	item.startProcess("moveToPosition_FollowingVectors3d_DuringTime", endPosition, new Vector(), time);

				loc.x = loc.x - this.getSpacing() - item.getSize().x;
				end = new Vector3d( loc );
			}
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
