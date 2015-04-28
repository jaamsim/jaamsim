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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

public class Queue extends LinkedComponent {

	@Keyword(description = "The priority for positioning the received entity in the queue.\n" +
			"Priority is integer valued and a lower numerical value indicates a higher priority.\n" +
			"For example, priority 3 is higher than 4, and priorities 3, 3.2, and 3.8 are equivalent.",
	         example = "Queue1 Priority { this.obj.Attrib1 }")
	private final SampleExpInput priority;

	@Keyword(description = "An expression that returns a dimensionless integer value that can be used to "
			+ "match entities in separate queues. The expression is evaluated when the entity "
			+ "first arrives at the queue. Since Match is integer valued, a value of 3.2 for one "
			+ "queue and 3.6 for another queue are considered to be equal.",
	         example = "Queue1 Match { this.obj.Attrib1 }")
	private final SampleExpInput match;

	@Keyword(description = "Determines the order in which entities are placed in the queue (FIFO or LIFO):\n" +
			"TRUE = first in first out (FIFO) order (the default setting)," +
			"FALSE = last in first out (LIFO) order.",
	         example = "Queue1 FIFO { FALSE }")
	private final BooleanInput fifo;

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         example = "Queue1 Spacing { 1 m }")
	private final ValueInput spacing;

	@Keyword(description = "The number of queuing entities in each row.",
			example = "Queue1 MaxPerLine { 4 }")
	protected final IntegerInput maxPerLine; // maximum items per sub line-up of queue

	protected ArrayList<QueueEntry> itemList;
	private final ArrayList<QueueUser> userList;  // other objects that use this queue

	//	Statistics
	protected double timeOfLastUpdate; // time at which the statistics were last updated
	protected double startOfStatisticsCollection; // time at which statistics collection was started
	protected int minElements; // minimum observed number of entities in the queue
	protected int maxElements; // maximum observed number of entities in the queue
	protected double elementSeconds;  // total time that entities have spent in the queue
	protected double squaredElementSeconds;  // total time for the square of the number of elements in the queue
	protected DoubleVector queueLengthDist;  // entry at position n is the total time the queue has had length n

	{
		testEntity.setHidden(true);
		nextComponent.setHidden(true);

		priority = new SampleExpInput("Priority", "Key Inputs", new SampleConstant(0));
		priority.setUnitType(DimensionlessUnit.class);
		priority.setEntity(this);
		priority.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(priority);

		match = new SampleExpInput("Match", "Key Inputs", new SampleConstant(0));
		match.setUnitType(DimensionlessUnit.class);
		match.setEntity(this);
		this.addInput(match);

		fifo = new BooleanInput("FIFO", "Key Inputs", true);
		this.addInput(fifo);

		spacing = new ValueInput("Spacing", "Key Inputs", 0.0d);
		spacing.setUnitType(DistanceUnit.class);
		spacing.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacing);

		maxPerLine = new IntegerInput("MaxPerLine", "Key Inputs", Integer.MAX_VALUE);
		maxPerLine.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(maxPerLine);
	}

	@Override
	public void validate() {
		super.validate();
		priority.validate();
	}

	public Queue() {
		itemList = new ArrayList<>();
		queueLengthDist = new DoubleVector(10,10);
		userList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemList.clear();

		// Clear statistics
		this.clearStatistics();

		// Identify the objects that use this queue
		userList.clear();
		for (Entity each : Entity.getAll()) {
			if (each instanceof QueueUser) {
				QueueUser u = (QueueUser)each;
				if (u.getQueues().contains(this))
					userList.add(u);
			}
		}
	}

	private static class QueueEntry {
		DisplayEntity entity;
		double timeAdded;
		int priority;
		int match;
	}

	private final DoQueueChanged userUpdate = new DoQueueChanged(this);
	private final EventHandle userUpdateHandle = new EventHandle();
	private static class DoQueueChanged extends ProcessTarget {
		private final Queue queue;

		public DoQueueChanged(Queue q) {
			queue = q;
		}

		@Override
		public void process() {
			for (QueueUser each : queue.userList)
				each.queueChanged();
		}

		@Override
		public String getDescription() {
			return queue.getName() + ".UpdateAllQueueUsers";
		}
	}

	// ******************************************************************************************************
	// QUEUE HANDLING METHODS
	// ******************************************************************************************************

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Determine the entity's priority and match values
		int pri = (int) priority.getValue().getNextSample(getSimTime());
		int mtch = (int) match.getValue().getNextSample(getSimTime());

		// Insert the entity in the correct position in the queue
		// FIFO ordering
		int pos = 0;
		if (fifo.getValue()) {
			for (int i=itemList.size()-1; i>=0; i--) {
				if (itemList.get(i).priority <= pri) {
					pos = i+1;
					break;
				}
			}
		}
		// LIFO ordering
		else {
			pos = itemList.size();
			for (int i=0; i<itemList.size(); i++) {
				if (itemList.get(i).priority >= pri) {
					pos = i;
					break;
				}
			}
		}
		this.add(pos, ent, pri, mtch);

		// Notify the users of this queue
		if (!userUpdateHandle.isScheduled())
			EventManager.scheduleTicks(0, 2, false, userUpdate, userUpdateHandle);
	}

	/**
	 * Inserts the specified element at the specified position in this Queue.
	 * Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
	 */
	private void add(int i, DisplayEntity ent, int pri, int mtch) {

		int queueSize = itemList.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize+1);

		QueueEntry entry = new QueueEntry();
		entry.entity = ent;
		entry.timeAdded = this.getSimTime();
		entry.priority = pri;
		entry.match = mtch;
		itemList.add(i, entry);
	}

	public void add(int i, DisplayEntity ent) {
		int pri = (int) priority.getValue().getNextSample(getSimTime());
		int mtch = (int) match.getValue().getNextSample(getSimTime());
		this.add(i, ent, pri, mtch);
	}

	/**
	 * Add an entity to the end of the queue
	 */
	public void addLast(DisplayEntity ent) {
		int pri = (int) priority.getValue().getNextSample(getSimTime());
		int mtch = (int) match.getValue().getNextSample(getSimTime());
		this.add(itemList.size(), ent, pri, mtch);
	}

	/**
	 * Removes the entity at the specified position in the queue
	 */
	public DisplayEntity remove(int i) {
		if (i >= itemList.size() || i < 0)
			error("Index: %d is beyond the end of the queue.", i);

		int queueSize = itemList.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize-1);

		QueueEntry entry = itemList.remove(i);
		DisplayEntity ent = entry.entity;
		this.incrementNumberProcessed();
		return ent;
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		return this.remove(0);
	}

	/**
	 * Returns the first entity in the queue.
	 * @return first entity in the queue.
	 */
	public DisplayEntity getFirst() {
		return itemList.get(0).entity;
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
		return this.getSimTime() - itemList.get(0).timeAdded;
	}

	/**
	 * Returns the number of times that the specified match value appears in
	 * the queue. If the match value is null, then every entity is counted.
	 * @param m - value to be matched.
	 * @return number of entities that have this match value.
	 */
	public int getMatchCount(Integer m) {

		if (m == null)
			return getCount();

		int ret = 0;
		for (QueueEntry item : itemList) {
			if (item.match == m)
				ret++;
		}
		return ret;
	}

	/**
	 * Returns the first entity in the queue whose match value is equal to the
	 * specified value. The returned entity is removed from the queue.
	 * If the match value is null, the first entity is removed.
	 * @param m - value to be matched.
	 * @return entity whose match value equals the specified value.
	 */
	public DisplayEntity removeFirstForMatch(Integer m) {

		if (m == null)
			return this.removeFirst();

		for (int i=0; i<itemList.size(); i++) {
			if (itemList.get(i).match == m)
				return this.remove(i);
		}
		return null;
	}

	/**
	 * Returns a match value that has sufficient numbers of entities in each
	 * queue. The first match value that satisfies the criterion is selected.
	 * If the numberList is too short, then the last value is used.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @return match value.
	 */
	public static Integer selectMatchValue(ArrayList<Queue> queueList, IntegerVector numberList) {

		// Find the shortest queue
		Queue shortest = null;
		int count = -1;
		for (Queue que : queueList) {
			if (que.getCount() > count) {
				count = que.getCount();
				shortest = que;
			}
		}

		// Return the first match value that has sufficient entities in each queue
		for (int m : shortest.getMatchValues(0.0)) {
			if (Queue.sufficientEntities(queueList, numberList, m))
				return m;
		}
		return null;
	}

	/**
	 * Returns true if each of the queues contains sufficient entities with
	 * the specified match value for processing to begin.
	 * If the numberList is too short, then the last value is used.
	 * If the numberList is null, then one entity per queue is required.
	 * If the match value m is null, then all the entities in each queue are counted.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @param m - match value.
	 * @return true if there are sufficient entities in each queue.
	 */
	public static boolean sufficientEntities(ArrayList<Queue> queueList, IntegerVector numberList, Integer m) {
		int number;
		for (int i=0; i<queueList.size(); i++) {
			if (numberList == null) {
				number = 1;
			}
			else {
				int ind = Math.min(i, numberList.size()-1);
				number = numberList.get(ind);
			}
			if (queueList.get(i).getMatchCount(m) < number)
				return false;
		}
		return true;
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	@Override
	public void updateGraphics(double simTime) {

		Vec3d queueOrientation = getOrientation();
		Vec3d qSize = this.getSize();
		Vec3d tmp = new Vec3d();

		double distanceX = 0.5d * qSize.x;
		double distanceY = 0;
		double maxWidth = 0;

		// find widest vessel
		if (itemList.size() >  maxPerLine.getValue()){
			for (QueueEntry entry : itemList) {
				 maxWidth = Math.max(maxWidth, entry.entity.getSize().y);
			 }
		}

		// update item locations
		for (int i = 0; i < itemList.size(); i++) {

			// if new row is required, set reset distanceX and move distanceY up one row
			if( i > 0 && i % maxPerLine.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacing.getValue() + maxWidth;
			}

			DisplayEntity item = itemList.get(i).entity;
			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			Vec3d itemSize = item.getSize();
			distanceX += spacing.getValue() + 0.5d * itemSize.x;
			tmp.set3(-distanceX / qSize.x, distanceY/qSize.y, 0.0d);

			// increment total distance
			distanceX += 0.5d * itemSize.x;

			// Set Position
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			item.setGlobalPositionForAlignment(new Vec3d(), itemCenter);
		}
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	/**
	 * Clear queue statistics
	 */
	@Override
	public void clearStatistics() {
		double simTime = this.getSimTime();
		startOfStatisticsCollection = simTime;
		timeOfLastUpdate = simTime;
		minElements = itemList.size();
		maxElements = itemList.size();
		elementSeconds = 0.0;
		squaredElementSeconds = 0.0;
		queueLengthDist.clear();
	}

	private void updateStatistics(int oldValue, int newValue) {

		minElements = Math.min(newValue, minElements);
		maxElements = Math.max(newValue, maxElements);

		// Add the necessary number of additional bins to the queue length distribution
		int n = newValue + 1 - queueLengthDist.size();
		for (int i = 0; i < n; i++) {
			queueLengthDist.add(0.0);
		}

		double simTime = this.getSimTime();
		double dt = simTime - timeOfLastUpdate;
		if (dt > 0.0) {
			elementSeconds += dt * oldValue;
			squaredElementSeconds += dt * oldValue * oldValue;
			queueLengthDist.addAt(dt,oldValue);  // add dt to the entry at index queueSize
			timeOfLastUpdate = simTime;
		}
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "QueueLength",
	 description = "The present number of entities in the queue.",
	    unitType = DimensionlessUnit.class)
	public double getQueueLength(double simTime) {
		return itemList.size();
	}

	@Output(name = "QueueTimes",
	 description = "The waiting time for each entity in the queue.",
	    unitType = TimeUnit.class)
	public ArrayList<Double> getQueueTimes(double simTime) {
		ArrayList<Double> ret = new ArrayList<>(itemList.size());
		for (QueueEntry item : itemList) {
			ret.add(simTime - item.timeAdded);
		}
		return ret;
	}

	@Output(name = "PriorityValues",
	 description = "The Priority expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class)
	public ArrayList<Integer> getPriorityValues(double simTime) {
		ArrayList<Integer> ret = new ArrayList<>();
		for (QueueEntry item : itemList) {
			ret.add(item.priority);
		}
		return ret;
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class)
	public ArrayList<Integer> getMatchValues(double simTime) {
		ArrayList<Integer> ret = new ArrayList<>();
		for (QueueEntry item : itemList) {
			ret.add(item.match);
		}
		return ret;
	}

	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getQueueLengthAverage(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			return (elementSeconds + dt*queueSize)/totalTime;
		}
		return 0.0;
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public double getQueueLengthStandardDeviation(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double mean = this.getQueueLengthAverage(simTime);
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			return Math.sqrt( (squaredElementSeconds + dt*queueSize*queueSize)/totalTime - mean*mean );
		}
		return 0.0;
	}

	@Output(name = "QueueLengthMinimum",
	 description = "The minimum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public Integer getQueueLengthMinimum(double simTime) {
		return minElements;
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public Integer getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		if (maxElements == 1 && queueLengthDist.get(1) == 0.0)
			return 0;
		return maxElements;
	}

	@Output(name = "QueueLengthDistribution",
	 description = "The fraction of time that the queue has length 0, 1, 2, etc.",
	    unitType = DimensionlessUnit.class,
	  reportable = true)
	public DoubleVector getQueueLengthDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(queueLengthDist);
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			if (ret.size() == 0)
				ret.add(0.0);
			ret.addAt(dt, queueSize);  // adds dt to the entry at index queueSize
			for (int i = 0; i < ret.size(); i++) {
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
	    unitType = TimeUnit.class,
	  reportable = true)
	public double getAverageQueueTime(double simTime) {
		int n = this.getNumberAdded();
		if (n == 0)
			return 0.0;
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemList.size();
		return (elementSeconds + dt*queueSize)/n;
	}

}
