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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

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
	         exampleList = {"this.obj.Attrib1"})
	private final SampleExpInput priority;

	@Keyword(description = "An expression that returns a dimensionless integer value that can be used to "
			+ "match entities in separate queues. The expression is evaluated when the entity "
			+ "first arrives at the queue. Since Match is integer valued, a value of 3.2 for one "
			+ "queue and 3.6 for another queue are considered to be equal.",
	         exampleList = {"this.obj.Attrib1"})
	private final SampleExpInput match;

	@Keyword(description = "Determines the order in which entities are placed in the queue (FIFO or LIFO):\n" +
			"TRUE = first in first out (FIFO) order (the default setting)," +
			"FALSE = last in first out (LIFO) order.",
	         exampleList = {"FALSE"})
	private final BooleanInput fifo;

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in the queue.",
	         exampleList = {"1 m"})
	private final ValueInput spacing;

	@Keyword(description = "The number of queuing entities in each row.",
			exampleList = {"4"})
	protected final IntegerInput maxPerLine; // maximum items per sub line-up of queue

	private final TreeSet<QueueEntry> itemSet;  // contains all the entities in queue order
	private final HashMap<Integer, TreeSet<QueueEntry>> matchMap; // each TreeSet contains the queued entities for a given match value

	private Integer matchForMaxCount;  // match value with the largest number of entities
	private int maxCount;     // largest number of entities for a given match value

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

		match = new SampleExpInput("Match", "Key Inputs", null);
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

	public Queue() {
		itemSet = new TreeSet<>();
		queueLengthDist = new DoubleVector(10,10);
		userList = new ArrayList<>();
		matchMap = new HashMap<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemSet.clear();
		matchMap.clear();

		matchForMaxCount = null;
		maxCount = -1;

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

	private static class QueueEntry implements Comparable<QueueEntry> {
		final DisplayEntity entity;
		final long entNum;
		final int priority;
		final Integer match;
		final double timeAdded;
		final Vec3d orientation;

		public QueueEntry(DisplayEntity ent, long n, int pri, Integer m, double t, Vec3d orient) {
			entity = ent;
			entNum = n;
			priority = pri;
			match = m;
			timeAdded = t;
			orientation = orient;
		}

		@Override
		public int compareTo(QueueEntry entry) {
			if (this.priority > entry.priority)
				return 1;
			else if (this.priority < entry.priority)
				return -1;
			else {
				if (this.entNum > entry.entNum)
					return 1;
				else if (this.entNum < entry.entNum)
					return -1;
				return 0;
			}
		}
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

		// Update the queue statistics
		int queueSize = itemSet.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize+1);

		// Build the entry for the entity
		// Note that the match value logic relies on all Integer objects with
		// the same int value having the same object reference
		long n = this.getNumberAdded();
		if (!fifo.getValue())
			n *= -1;
		int pri = (int) priority.getValue().getNextSample(getSimTime());
		Integer m = null;
		if (match.getValue() != null)
			m = Integer.valueOf((int) match.getValue().getNextSample(getSimTime()));
		QueueEntry entry = new QueueEntry(ent, n, pri, m, getSimTime(), ent.getOrientation());

		// Add the entity to the TreeSet of all the entities in the queue
		itemSet.add(entry);

		// Does the entry have a match value?
		if (entry.match != null) {

			// Add the entity to the TreeSet of all the entities with this match value
			TreeSet<QueueEntry> matchSet = matchMap.get(entry.match);
			if (matchSet == null) {
				matchSet = new TreeSet<>();
				matchSet.add(entry);
				matchMap.put(entry.match, matchSet);
			}
			else {
				matchSet.add(entry);
			}

			// Update the maximum count
			if (entry.match == matchForMaxCount) {
				maxCount++;
			}
			else {
				if (matchSet.size() > maxCount) {
					matchForMaxCount = entry.match;
					maxCount = matchSet.size();
				}
			}
		}

		// Notify the users of this queue
		if (!userUpdateHandle.isScheduled())
			EventManager.scheduleTicks(0, 2, false, userUpdate, userUpdateHandle);
	}

	/**
	 * Removes a specified entity from the queue
	 */
	public DisplayEntity remove(QueueEntry entry) {

		int queueSize = itemSet.size();  // present number of entities in the queue
		this.updateStatistics(queueSize, queueSize-1);

		// Remove the entity from the TreeSet of all entities in the queue
		boolean found = itemSet.remove(entry);
		if (!found)
			error("Cannot find the entry in itemSet.");

		// Does the entry have a match value?
		if (entry.match != null) {

			// Remove the entity from the TreeSet for that match value
			TreeSet<QueueEntry> matchSet = matchMap.get(entry.match);
			if (matchSet == null)
				error("Cannot find an entry in matchMap for match value: %s", entry.match);
			found = matchSet.remove(entry);
			if (!found)
				error("Cannot find the entry in matchMap.");

			// If there are no more entities for this match value, remove it from the HashMap of match values
			if (matchSet.isEmpty())
				matchMap.remove(entry.match);

			// Update the maximum count
			if (entry.match == matchForMaxCount) {
				matchForMaxCount = null;
				maxCount = -1;
			}
		}

		// Reset the entity's orientation to its original value
		entry.entity.setOrientation(entry.orientation);

		this.incrementNumberProcessed();
		return entry.entity;
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		return this.remove(itemSet.first());
	}

	/**
	 * Returns the first entity in the queue.
	 * @return first entity in the queue.
	 */
	public DisplayEntity getFirst() {
		return itemSet.first().entity;
	}

	/**
	 * Returns the number of entities in the queue
	 */
	public int getCount() {
		return itemSet.size();
	}

	/**
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		return this.getSimTime() - itemSet.first().timeAdded;
	}

	/**
	 * Returns the number of times that the specified match value appears in
	 * the queue. If the match value is null, then every entity is counted.
	 * @param m - value to be matched.
	 * @return number of entities that have this match value.
	 */
	public int getMatchCount(Integer m) {
		if (m == null)
			return itemSet.size();
		TreeSet<QueueEntry> matchSet = matchMap.get(m);
		if (matchSet == null)
			return 0;
		return matchSet.size();
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

		TreeSet<QueueEntry> matchSet = matchMap.get(m);
		if (matchSet == null)
			return null;
		return this.remove(matchSet.first());
	}

	public ArrayList<Integer> getUniqueMatchValues() {
		ArrayList<Integer> ret = new ArrayList<>(matchMap.size());
		Iterator<Integer> itr = matchMap.keySet().iterator();
		while (itr.hasNext()) {
			ret.add(itr.next());
		}
		return ret;
	}

	/**
	 * Returns the match value that has the largest number of entities in the queue.
	 * @return match value with the most entities.
	 */
	public int getMatchForMax() {
		if (matchForMaxCount == null)
			this.setMaxCount();
		return matchForMaxCount;
	}

	/**
	 * Returns the number of entities in the longest match value queue.
	 * @return number of entities in the longest match value queue.
	 */
	public int getMaxCount() {
		if (matchForMaxCount == null)
			this.setMaxCount();
		return maxCount;
	}

	/**
	 * Determines the longest queue for a give match value.
	 */
	private void setMaxCount() {
		maxCount = -1;
		Iterator<Integer> itr = matchMap.keySet().iterator();
		while (itr.hasNext()) {
			Integer m = itr.next();
			int count = matchMap.get(m).size();
			if (count > maxCount) {
				maxCount = count;
				matchForMaxCount = m;
			}
		}
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

		// Check whether each queue has sufficient entities for any match value
		int number;
		for (int i=0; i<queueList.size(); i++) {
			if (numberList == null) {
				number = 1;
			}
			else {
				int ind = Math.min(i, numberList.size()-1);
				number = numberList.get(ind);
			}
			if (queueList.get(i).getMaxCount() < number)
				return null;
		}

		// Find the queue with the fewest match values
		Queue shortest = null;
		int count = -1;
		for (Queue que : queueList) {
			int n = (int) que.getMatchValueCount(0.0);
			if (n > count) {
				count = n;
				shortest = que;
			}
		}

		// Return the first match value that has sufficient entities in each queue
		for (int m : shortest.getUniqueMatchValues()) {
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
		if (itemSet.size() >  maxPerLine.getValue()){
			Iterator<QueueEntry> itr = itemSet.iterator();
			while (itr.hasNext()) {
				 maxWidth = Math.max(maxWidth, itr.next().entity.getSize().y);
			 }
		}

		// update item locations
		int i = 0;
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			DisplayEntity item = itr.next().entity;

			// if new row is required, set reset distanceX and move distanceY up one row
			i++;
			if( i > 0 && i % maxPerLine.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacing.getValue() + maxWidth;
			}

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
		minElements = itemSet.size();
		maxElements = itemSet.size();
		elementSeconds = 0.0;
		squaredElementSeconds = 0.0;
		for (int i=0; i<queueLengthDist.size(); i++) {
			queueLengthDist.set(i, 0.0d);
		}
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
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public double getQueueLength(double simTime) {
		return itemSet.size();
	}

	@Output(name = "QueueTimes",
	 description = "The waiting time for each entity in the queue.",
	    unitType = TimeUnit.class,
	    sequence = 1)
	public ArrayList<Double> getQueueTimes(double simTime) {
		ArrayList<Double> ret = new ArrayList<>(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(simTime - itr.next().timeAdded);
		}
		return ret;
	}

	@Output(name = "PriorityValues",
	 description = "The Priority expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public ArrayList<Integer> getPriorityValues(double simTime) {
		ArrayList<Integer> ret = new ArrayList<>();
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().priority);
		}
		return ret;
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public ArrayList<Integer> getMatchValues(double simTime) {
		ArrayList<Integer> ret = new ArrayList<>();
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().match);
		}
		return ret;
	}


	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	  sequence = 4)
	public double getQueueLengthAverage(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemSet.size();
		double totalTime = simTime - startOfStatisticsCollection;
		if (totalTime > 0.0) {
			return (elementSeconds + dt*queueSize)/totalTime;
		}
		return 0.0;
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	  sequence = 5)
	public double getQueueLengthStandardDeviation(double simTime) {
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemSet.size();
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
	  reportable = true,
	  sequence = 6)
	public Integer getQueueLengthMinimum(double simTime) {
		return minElements;
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	  sequence = 7)
	public Integer getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		if (maxElements == 1 && queueLengthDist.get(1) == 0.0)
			return 0;
		return maxElements;
	}

	@Output(name = "QueueLengthTimes",
	 description = "The total time that the queue has length 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	  sequence = 8)
	public DoubleVector getQueueLengthDistribution(double simTime) {
		DoubleVector ret = new DoubleVector(queueLengthDist);
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemSet.size();
		if (ret.size() == 0)
			ret.add(0.0);
		ret.addAt(dt, queueSize);
		return ret;
	}

	@Output(name = "AverageQueueTime",
	 description = "The average time each entity waits in the queue.  Calculated as total queue time to date divided " +
			"by the total number of entities added to the queue.",
	    unitType = TimeUnit.class,
	  reportable = true,
	  sequence = 9)
	public double getAverageQueueTime(double simTime) {
		int n = this.getNumberAdded();
		if (n == 0)
			return 0.0;
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemSet.size();
		return (elementSeconds + dt*queueSize)/n;
	}

	@Output(name = "MatchValueCount",
	 description = "The present number of unique match values in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 10)
	public double getMatchValueCount(double simTime) {
		return matchMap.size();
	}

}
