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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.IntegerInput;

public class Queue extends DisplayEntity {

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         example = "Queue1 Spacing { 1 }")
	private final ValueInput spacingInput;

	@Keyword(description = "The number of queuing entities in each row.",
			example = "Queue1 MaxPerLine { 4 }")
	protected final IntegerInput maxPerLineInput; // maximum items per sub line-up of queue

	protected ArrayList<DisplayEntity> itemList;
	private ArrayList<Double> timeAddedList;

	//	Statistics
	protected double timeOfLastUpdate; // time at which the statistics were last updated
	protected double startOfStatisticsCollection; // time at which statistics collection was started
	protected int minElements; // minimum observed number of entities in the queue
	protected int maxElements; // maximum observed number of entities in the queue
	protected double elementSeconds;  // total time that entities have spent in the queue
	protected double squaredElementSeconds;  // total time for the square of the number of elements in the queue
	protected int numberAdded;    // number of entities that have been added to the queue
	protected int numberRemoved;  // number of entities that have been removed from the queue
	protected DoubleVector queueLengthDist;  // entry at position n is the total time the queue has had length n
	protected ArrayList<QueueRecorder> recorderList;

	{
		spacingInput = new ValueInput("Spacing", "Key Inputs", 0.0d);
		spacingInput.setUnitType(DistanceUnit.class);
		spacingInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacingInput);

		maxPerLineInput = new IntegerInput("MaxPerLine", "Key Inputs", Integer.MAX_VALUE);
		maxPerLineInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(maxPerLineInput);
	}

	public Queue() {
		itemList = new ArrayList<DisplayEntity>();
		timeAddedList = new ArrayList<Double>();
		queueLengthDist = new DoubleVector(10,10);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemList.clear();
		timeAddedList.clear();

		// Clear statistics
		this.clearStatistics();

		recorderList = new ArrayList<QueueRecorder>();
		for( QueueRecorder rec : Entity.getClonesOfIterator( QueueRecorder.class ) ) {
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
		this.updateStatistics();  // update the queue length distribution
		itemList.add( i, perf );
		timeAddedList.add( i, this.getSimTime() );
		this.updateStatistics();  // update the min and max queue length
		numberAdded++;

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
			this.updateStatistics();  // update the queue length distribution
			DisplayEntity out = itemList.remove(i);
			//double queueTime = this.getSimTime() - timeAddedList.remove(i);
			timeAddedList.remove(i);
			this.updateStatistics();  // update the min and max queue length
			numberRemoved++;

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
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		return this.getSimTime() - timeAddedList.get(0);
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
		double simTime = this.getSimTime();
		startOfStatisticsCollection = simTime;
		timeOfLastUpdate = simTime;
		minElements = itemList.size();
		maxElements = itemList.size();
		elementSeconds = 0.0;
		squaredElementSeconds = 0.0;
		numberAdded = 0;
		numberRemoved = 0;
		queueLengthDist.clear();
	}

	public void updateStatistics() {

		int queueSize = itemList.size();  // present number of entities in the queue
		minElements = Math.min(queueSize, minElements);
		maxElements = Math.max(queueSize, maxElements);

		// Add the necessary number of additional bins to the queue length distribution
		int n = queueSize + 1 - queueLengthDist.size();
		for( int i=0; i<n; i++ ) {
			queueLengthDist.add(0.0);
		}

		double simTime = this.getSimTime();
		double dt = simTime - timeOfLastUpdate;
		if( dt > 0.0 ) {
			elementSeconds += dt * queueSize;
			squaredElementSeconds += dt * queueSize * queueSize;
			queueLengthDist.addAt(dt,queueSize);  // add dt to the entry at index queueSize
			timeOfLastUpdate = simTime;
		}
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	public void printUtilizationOn( FileEntity anOut ) {

		if (isActive()) {
			anOut.format( "%s\t", getInputName() );
			anOut.format( "%d\t", this.getQueueLengthMinimum(0.0) );
			anOut.format( "%d\t", this.getQueueLengthMaximum(0.0) );
			anOut.format( "%.0f\t", this.getQueueLength(0.0) );
			anOut.format( "\n" );
		}
	}

	public void printUtilizationHeaderOn( FileEntity anOut ) {
		anOut.format( "Name\t" );
		anOut.format( "Min Elements\t" );
		anOut.format( "Max Elements\t" );
		anOut.format( "Present Elements\t" );
		anOut.format( "\n" );
	}

	@Output(name = "NumberAdded",
	 description = "The number of entities that have been added to the queue.",
	    unitType = DimensionlessUnit.class)
	public Integer getNumberAdded(double simTime) {
		return numberAdded;
	}

	@Output(name = "NumberRemoved",
	 description = "The number of entities that have been removed from the queue.",
	    unitType = DimensionlessUnit.class)
	public Integer getNumberRemoved(double simTime) {
		return numberRemoved;
	}

	@Output(name = "QueueLength",
	 description = "The present number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public double getQueueLength(double simTime) {
		return itemList.size();
	}

	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public double getQueueLengthAverage(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if( totalTime > 0.0 ) {
			return (elementSeconds + dt*queueSize)/totalTime;
		}
		return 0.0;
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public double getQueueLengthStandardDeviation(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double mean = this.getQueueLengthAverage(simTime);
		double totalTime = simTime - startOfStatisticsCollection;
		if( totalTime > 0.0 ) {
			return Math.sqrt( (squaredElementSeconds + dt*queueSize*queueSize)/totalTime - mean*mean );
		}
		return 0.0;
	}

	@Output(name = "QueueLengthMinimum",
	 description = "The minimum number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public Integer getQueueLengthMinimum(double simTime) {
		return minElements;
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public Integer getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		if( maxElements == 1 && queueLengthDist.get(1) == 0.0 )
			return 0;
		return maxElements;
	}

	@Output(name = "QueueLengthDistribution",
	 description = "The fraction of time that the queue has length 0, 1, 2, etc.",
	    unitType = DimensionlessUnit.class)
	public DoubleVector getQueueLengthDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(queueLengthDist);
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if( totalTime > 0.0 ) {
			if( ret.size() == 0 )
				ret.add(0.0);
			ret.addAt(dt, queueSize);  // adds dt to the entry at index queueSize
			for( int i=0; i<ret.size(); i++ ) {
				ret.set(i, ret.get(i)/totalTime);
			}
		}
		else {
			ret.clear();
		}
		return ret;
	}

	@Output(name = "AverageQueueTime",
	 description = "The average time each entity waits in the queue.  Calculated as total queue time to date divided " +
			"by the total number of entities added to the queue.",
	    unitType = TimeUnit.class)
	public double getAverageQueueTime(double simTime) {
		if( numberAdded == 0 )
			return 0.0;
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		return (elementSeconds + dt*queueSize)/numberAdded;
	}

}
