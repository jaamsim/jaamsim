/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.InterfaceEntityInput;
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
	private final SampleInput priority;

	@Keyword(description = "An expression that returns a dimensionless integer value that can be used to "
			+ "match entities in separate queues. The expression is evaluated when the entity "
			+ "first arrives at the queue. Since Match is integer valued, a value of 3.2 for one "
			+ "queue and 3.6 for another queue are considered to be equal.",
	         exampleList = {"this.obj.Attrib1"})
	private final SampleInput match;

	@Keyword(description = "Determines the order in which entities are placed in the queue (FIFO or LIFO):\n" +
			"TRUE = first in first out (FIFO) order (the default setting)," +
			"FALSE = last in first out (LIFO) order.",
	         exampleList = {"FALSE"})
	private final BooleanInput fifo;

	@Keyword(description = "The time an entity will wait in the queue before deciding whether or "
	                     + "not to renege. Evaluated when the entity first enters the queue.\n"
	                     + "A constant value, a distribution to be sampled, a time series, or an "
	                     + "expression can be entered.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput renegeTime;

	@Keyword(description = "A logical condition that determines whether an entity will renege "
	                     + "after waiting for its RenegeTime value. Note that TRUE and FALSE are "
	                     + "entered as 1 and 0, respectively.\n"
	                     + "A constant value, a distribution to be sampled, a time series, or an "
	                     + "expression can be entered.",
	         exampleList = { "1", "'this.QueuePosition > 1'", "'this.QueuePostion > [Queue2].QueueLength'" })
	private final SampleInput renegeCondition;

	@Keyword(description = "The object to which an entity will be sent if it reneges.",
	         exampleList = {"Branch1"})
	protected final InterfaceEntityInput<Linkable> renegeDestination;

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
	protected long numberReneged;  // number of entities that reneged from the queue

	{
		defaultEntity.setHidden(true);
		nextComponent.setHidden(true);

		priority = new SampleInput("Priority", "Key Inputs", new SampleConstant(0));
		priority.setUnitType(DimensionlessUnit.class);
		priority.setEntity(this);
		priority.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(priority);

		match = new SampleInput("Match", "Key Inputs", null);
		match.setUnitType(DimensionlessUnit.class);
		match.setEntity(this);
		this.addInput(match);

		fifo = new BooleanInput("FIFO", "Key Inputs", true);
		this.addInput(fifo);

		renegeTime = new SampleInput("RenegeTime", "Key Inputs", null);
		renegeTime.setUnitType(TimeUnit.class);
		renegeTime.setEntity(this);
		renegeTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(renegeTime);

		renegeCondition = new SampleInput("RenegeCondition", "Key Inputs", new SampleConstant(1));
		renegeCondition.setUnitType(DimensionlessUnit.class);
		renegeCondition.setEntity(this);
		renegeCondition.setValidRange(0.0d, 1.0d);
		this.addInput(renegeCondition);

		renegeDestination = new InterfaceEntityInput<>(Linkable.class, "RenegeDestination", "Key Inputs", null);
		this.addInput(renegeDestination);

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
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == renegeTime) {
			boolean bool = renegeTime.getValue() != null;
			renegeDestination.setRequired(bool);
			return;
		}
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
		startOfStatisticsCollection = 0.0;
		timeOfLastUpdate = 0.0;
		minElements = 0;
		maxElements = 0;
		elementSeconds = 0.0;
		squaredElementSeconds = 0.0;
		queueLengthDist.clear();
		numberReneged = 0;

		// Identify the objects that use this queue
		userList.clear();
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
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
		long n = this.getTotalNumberAdded();
		if (!fifo.getValue())
			n *= -1;
		int pri = (int) priority.getValue().getNextSample(getSimTime());
		Integer m = null;
		if (match.getValue() != null)
			m = Integer.valueOf((int) match.getValue().getNextSample(getSimTime()));
		QueueEntry entry = new QueueEntry(ent, n, pri, m, getSimTime(), ent.getOrientation());

		// Add the entity to the TreeSet of all the entities in the queue
		boolean bool = itemSet.add(entry);
		if (!bool)
			error("Entity %s is already present in the queue.", ent);

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
			if (entry.match.equals(matchForMaxCount)) {
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

		// Schedule the time to check the renege condition
		if (renegeTime.getValue() != null) {
			double dur = renegeTime.getValue().getNextSample(getSimTime());
			// Schedule the renege tests in FIFO order so that if two or more entities are added to
			// the queue at the same time, the one nearest the front of the queue is tested first
			EventManager.scheduleSeconds(dur, 5, true, new RenegeActionTarget(this, ent), null);
		}
	}

	private static class RenegeActionTarget extends EntityTarget<Queue> {
		private final DisplayEntity queuedEnt;

		RenegeActionTarget(Queue q, DisplayEntity e) {
			super(q, "renegeAction");
			queuedEnt = e;
		}

		@Override
		public void process() {
			ent.renegeAction(queuedEnt);
		}
	}

	public void renegeAction(DisplayEntity ent) {

		// Do nothing if the entity has already left the queue
		QueueEntry entry = this.getQueueEntry(ent);
		if (entry == null)
			return;

		// Temporarily set the obj entity to the one that might renege
		double simTime = this.getSimTime();
		DisplayEntity oldEnt = this.getReceivedEntity(simTime);
		this.setReceivedEntity(ent);

		// Check the condition for reneging
		if (renegeCondition.getValue().getNextSample(simTime) == 0.0d) {
			this.setReceivedEntity(oldEnt);
			return;
		}

		// Reset the obj entity to the original one
		this.setReceivedEntity(oldEnt);

		// Remove the entity from the queue and send it to the renege destination
		this.remove(entry);
		numberReneged++;
		renegeDestination.getValue().addEntity(ent);
	}

	public DisplayEntity removeEntity(DisplayEntity ent) {
		return this.remove(getQueueEntry(ent));
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
			if (entry.match.equals(matchForMaxCount)) {
				matchForMaxCount = null;
				maxCount = -1;
			}
		}

		// Reset the entity's orientation to its original value
		entry.entity.setOrientation(entry.orientation);

		this.incrementNumberProcessed();
		return entry.entity;
	}

	private QueueEntry getQueueEntry(DisplayEntity ent) {
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			QueueEntry entry = itr.next();
			if (entry.entity == ent)
				return entry;
		}
		return null;
	}

	/**
	 * Returns the position of the specified entity in the queue.
	 * Returns -1 if the entity is not found.
	 * @param ent - entity in question
	 * @return index of the entity in the queue.
	 */
	public int getPosition(DisplayEntity ent) {
		int ret = 0;
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			if (itr.next().entity == ent)
				return ret;

			ret++;
		}
		return -1;
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		if (itemSet.isEmpty())
			error("Cannot remove an entity from an empty queue");
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
	 * Returns true if the queue is empty
	 */
	public boolean isEmpty() {
		return itemSet.isEmpty();
	}

	/**
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		return this.getSimTime() - itemSet.first().timeAdded;
	}

	/**
	 * Returns the priority value for the first object in the queue
	 */
	public int getFirstPriority() {
		return itemSet.first().priority;
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
		for (Entry<Integer, TreeSet<QueueEntry>> each : matchMap.entrySet()) {
			int count = each.getValue().size();
			if (count > maxCount) {
				maxCount = count;
				matchForMaxCount = each.getKey();
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
			int n = que.getMatchValueCount(0.0);
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

		// Copy the item set to avoid some concurrent modification exceptions
		TreeSet<QueueEntry> itemSetCopy = new TreeSet<>(itemSet);

		// find widest vessel
		if (itemSetCopy.size() >  maxPerLine.getValue()){
			Iterator<QueueEntry> itr = itemSetCopy.iterator();
			while (itr.hasNext()) {
				 maxWidth = Math.max(maxWidth, itr.next().entity.getSize().y);
			 }
		}

		// update item locations
		int i = 0;
		Iterator<QueueEntry> itr = itemSetCopy.iterator();
		while (itr.hasNext()) {
			DisplayEntity item = itr.next().entity;

			// if new row is required, set reset distanceX and move distanceY up one row
			if( i > 0 && i % maxPerLine.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacing.getValue() + maxWidth;
			}
			i++;

			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			Vec3d itemSize = item.getSize();
			distanceX += spacing.getValue() + 0.5d * itemSize.x;
			tmp.set3(-distanceX / qSize.x, distanceY/qSize.y, 0.0d);

			// increment total distance
			distanceX += 0.5d * itemSize.x;

			// Set Position
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			item.setGlobalPositionForAlignment(item.getAlignment(), itemCenter);
		}
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	@Override
	public void clearStatistics() {
		super.clearStatistics();
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
		numberReneged = 0;
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

	// LinkDisplayable
	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = super.getDestinationEntities();
		Linkable l = renegeDestination.getValue();
		if (l != null && (l instanceof Entity)) {
			ret.add((Entity)l);
		}
		return ret;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "QueueLength",
	 description = "The present number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getQueueLength(double simTime) {
		return itemSet.size();
	}

	@Output(name = "QueueList",
	 description = "The entities in the queue.",
	    sequence = 1)
	public ArrayList<DisplayEntity> getQueueList(double simTime) {
		ArrayList<DisplayEntity> ret = new ArrayList<>(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().entity);
		}
		return ret;
	}

	@Output(name = "QueueTimes",
	 description = "The waiting time for each entity in the queue.",
	    unitType = TimeUnit.class,
	    sequence = 2)
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
	    sequence = 3)
	public IntegerVector getPriorityValues(double simTime) {
		IntegerVector ret = new IntegerVector(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().priority);
		}
		return ret;
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public IntegerVector getMatchValues(double simTime) {
		IntegerVector ret = new IntegerVector(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			Integer val = itr.next().match;
			if (val != null) {
				ret.add(val);
			}
		}
		return ret;
	}


	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
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
	    sequence = 6)
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
	    sequence = 7)
	public int getQueueLengthMinimum(double simTime) {
		return minElements;
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public int getQueueLengthMaximum(double simTime) {
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
	    sequence = 9)
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
	    sequence = 10)
	public double getAverageQueueTime(double simTime) {
		long n = this.getNumberAdded();
		if (n == 0)
			return 0.0;
		double dt = simTime - timeOfLastUpdate;
		int queueSize = itemSet.size();
		return (elementSeconds + dt*queueSize)/n;
	}

	@Output(name = "MatchValueCount",
	 description = "The present number of unique match values in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 11)
	public int getMatchValueCount(double simTime) {
		return matchMap.size();
	}

	@Output(name = "NumberReneged",
	 description = "The number of entities that reneged from the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 12)
	public long getNumberReneged(double simTime) {
		return numberReneged;
	}

	@Output(name = "QueuePosition",
	 description = "The position in the queue for an entity undergoing the RenegeCondition test.\n"
	             + "First in queue = 1, second in queue = 2, etc.",
	    unitType = DimensionlessUnit.class,
	  reportable = false,
	    sequence = 13)
	public long getQueuePosition(double simTime) {
		DisplayEntity objEnt = this.getReceivedEntity(simTime);
		if (objEnt == null)
			return -1;
		int pos = this.getPosition(objEnt);
		if (pos >= 0)
			pos++;
		return pos;
	}

}
