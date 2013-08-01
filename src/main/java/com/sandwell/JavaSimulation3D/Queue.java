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

import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;

public class Queue extends DisplayEntity {

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         example = "Queue-1 Spacing { 1 }")
	private final ValueInput spacingInput;

	@Keyword(description = "The number of queuing entities in each row.",
			example = "Queue-1 MaxPerLine { 4 }")
	protected final IntegerInput maxPerLineInput; // maximum items per sub line-up of queue

	protected ArrayList<DisplayEntity> itemList;

//	Statistics
	protected double minElements;
	protected int maxElements;
	protected double avgElements;
	protected ArrayList<QueueRecorder> recorderList;

	{
		spacingInput = new ValueInput("Spacing", "Key Inputs", 0.0d);
		spacingInput.setUnitType(DistanceUnit.class);
		spacingInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacingInput, true);

		maxPerLineInput = new IntegerInput("MaxPerLine", "Key Inputs", Integer.MAX_VALUE);
		maxPerLineInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(maxPerLineInput, true);
	}

	public Queue() {
		itemList = new ArrayList<DisplayEntity>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemList.clear();

		// Clear statistics
		this.clearStatistics();

		recorderList = new ArrayList<QueueRecorder>();
		for( QueueRecorder rec : Entity.getClonesOf( QueueRecorder.class ) ) {
			if( rec.getQueueList().contains( this ) ) {
				recorderList.add( rec );
			}
		}
	}

	// ******************************************************************************************************
	// QUEUE HANDLING METHODS
	// ******************************************************************************************************

	/**
	 * Inserts the specified element at the specified position in this Queue.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 */
	public void add( int i, DisplayEntity perf ) {
		this.updateStatistics();
		itemList.add( i, perf );
		this.updateStatistics();
		setGraphicsDataDirty();

		for( QueueRecorder rec : recorderList ) {
			rec.add( perf, this );
		}
	}

	/**
	 * Add an entity to the end of the queue
	 */
	public void addLast( DisplayEntity perf ) {
		this.add(itemList.size(), perf);
	}

	/**
	 * Removes the entity at the specified position in the queue
	 */
	public DisplayEntity remove(int i) {
		if( i < itemList.size() && i >= 0 ) {
			this.updateStatistics();
			DisplayEntity out = itemList.remove(i);
			this.updateStatistics();
			setGraphicsDataDirty();

			for( QueueRecorder rec : recorderList ) {
				rec.remove( out, this );
			}
			return out;
		}
		else {
			throw new ErrorException( " Index is beyond the end of the queue. " );
		}
	}

	/**
	 * Removes the specified entity from the queue
	 */
	public void remove( DisplayEntity perf ) {
		int i = itemList.indexOf(perf);
		if( i >= 0 )
			this.remove(i);

		else
			throw new ErrorException( "item not found in queue " );
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		return this.remove(0);
	}

	/**
	 * Removes the last entity from the queue
	 */
	public DisplayEntity removeLast() {
		return this.remove( itemList.size()-1 );
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
	@Override
	public void updateGraphics( double simTime ) {

		//int max = itemList.size();
		// If set, limit the number of items per sub-lane
		//if (maxPerLine > 0)
		//	max = maxPerLine;
		Vec3d queueOrientation = getOrientation();
		Vec3d qSize = this.getSize();
		Vec3d tmp = new Vec3d();

		double distanceX = 0.5d * qSize.x;
		double distanceY = 0;
		double maxWidth = 0;

		// find widest vessel
		if( itemList.size() >  maxPerLineInput.getValue()){
			for (int j = 0; j < itemList.size(); j++) {
				 maxWidth = Math.max(maxWidth, itemList.get(j).getSize().y);
			 }
		}
		// update item locations
		for (int i = 0; i < itemList.size(); i++) {

			// if new row is required, set reset distanceX and move distanceY up one row
			if( i > 0 && i % maxPerLineInput.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacingInput.getValue() + maxWidth;
			}

			DisplayEntity item = itemList.get(i);
			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			Vec3d itemSize = item.getSize();
			distanceX += spacingInput.getValue() + 0.5d * itemSize.x;
			tmp.set3(-distanceX / qSize.x, distanceY/qSize.y, 0.0d);

			// increment total distance
			distanceX += 0.5d * itemSize.x;

			// Set Position
			Vec3d itemCenter = this.getPositionForAlignment(tmp);
			item.setPositionForAlignment(new Vec3d(), itemCenter);
		}
	}

	public ArrayList<DisplayEntity> getItemList() {
		return itemList;
	}

	public double getPhysicalLength() {
		double length;

		length = 0.0;
		for( int x = 0; x < itemList.size(); x++ ) {
			DisplayEntity item = itemList.get( x );
			length += item.getSize().x + spacingInput.getValue();
		}
		return length;
	}

	/**
	 * Returns the position for a new entity at the end of the queue.
	 */
	public Vec3d getEndVector3dFor(DisplayEntity perf) {
		Vec3d qSize = this.getSize();
		double distance = 0.5d * qSize.x;
		for (int x = 0; x < itemList.size(); x++) {
			DisplayEntity item = itemList.get(x);
			distance += spacingInput.getValue() + item.getSize().x;
		}
		distance += spacingInput.getValue() + 0.5d * perf.getSize().x;
		Vec3d tempAlign = new Vec3d(-distance / qSize.x, 0.0d, 0.0d);
		return this.getPositionForAlignment(tempAlign);
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
}
