/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.EntStorage.StorageEntry;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

public class Queue extends LinkedComponent {

	@Keyword(description = "The priority for positioning the received entity in the queue. "
	                     + "Priority is integer valued and a lower numerical value indicates a "
	                     + "higher priority. "
	                     + "For example, priority 3 is higher than 4, and priorities 3, 3.2, and "
	                     + "3.8 are equivalent.",
	         exampleList = {"this.obj.Attrib1"})
	private final SampleInput priority;

	@Keyword(description = "An expression returning a string value that categorizes the queued "
	                     + "entities. The expression is evaluated and the value saved when the "
	                     + "entity first arrives at the queue. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer.",
	         exampleList = {"this.obj.Attrib1"})
	private final StringProvInput match;

	@Keyword(description = "Determines the order in which entities are placed in the queue (FIFO "
	                     + "or LIFO):\n"
	                     + "TRUE = first in first out (FIFO) order (the default setting),\n"
	                     + "FALSE = last in first out (LIFO) order.")
	private final BooleanProvInput fifo;

	@Keyword(description = "The time an entity will wait in the queue before deciding whether or "
	                     + "not to renege. Evaluated when the entity first enters the queue.",
	         exampleList = {"3.0 h", "NormalDistribution1",
	                        "'1[s] + 0.5*[TimeSeries1].PresentValue'"})
	private final SampleInput renegeTime;

	@Keyword(description = "A logical condition that determines whether an entity will renege "
	                     + "after waiting for its RenegeTime value. Note that TRUE and FALSE are "
	                     + "entered as 1 and 0, respectively.",
	         exampleList = {"1", "'this.QueuePosition > 1'",
	                        "'this.QueuePostion > [Queue2].QueueLength'"})
	private final SampleInput renegeCondition;

	@Keyword(description = "The object to which an entity will be sent if it reneges.",
	         exampleList = {"Branch1"})
	protected final InterfaceEntityInput<Linkable> renegeDestination;

	@Keyword(description = "Maximum number of objects that can be placed in the queue. "
	                     + "An error message is generated if this limit is exceeded.\n\n"
	                     + "This input is intended to trap a model error that causes the queue "
	                     + "length to grow without bound. "
	                     + "It has no effect on model logic.",
	         exampleList = {"100"})
	protected final SampleInput maxValidLength;

	@Keyword(description = "The amount of graphical space shown between objects in the queue.",
	         exampleList = {"1 m"})
	private final SampleInput spacing;

	@Keyword(description = "Maximum number of objects in each row of the Queue.",
			exampleList = {"4"})
	protected final SampleInput maxPerLine; // maximum items per sub line-up of queue

	@Keyword(description = "The number of rows in each level of the Queue.",
			exampleList = {"4"})
	protected final SampleInput maxRows;

	@Keyword(description = "If TRUE, the objects in the Queue are displayed.")
	protected final BooleanProvInput showEntities;

	private final EntStorage storage;  // stores the entities in the queue
	private final ArrayList<QueueUser> userList;  // other objects that use this queue
	private final TimeBasedStatistics stats;
	private final TimeBasedFrequency freq;
	protected long numberReneged;  // number of entities that reneged from the queue

	{
		defaultEntity.setHidden(true);
		nextComponent.setHidden(true);

		priority = new SampleInput("Priority", KEY_INPUTS, 0);
		priority.setUnitType(DimensionlessUnit.class);
		priority.setIntegerValue(true);
		priority.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		priority.setOutput(true);
		this.addInput(priority);

		match = new StringProvInput("Match", KEY_INPUTS, null);
		match.setUnitType(DimensionlessUnit.class);
		this.addInput(match);

		fifo = new BooleanProvInput("FIFO", KEY_INPUTS, true);
		this.addInput(fifo);

		renegeTime = new SampleInput("RenegeTime", KEY_INPUTS, Double.POSITIVE_INFINITY);
		renegeTime.setUnitType(TimeUnit.class);
		renegeTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		renegeTime.setCallback(inputCallback);
		renegeTime.setOutput(true);
		this.addInput(renegeTime);

		renegeCondition = new SampleInput("RenegeCondition", KEY_INPUTS, 1);
		renegeCondition.setUnitType(DimensionlessUnit.class);
		renegeCondition.setValidRange(0.0d, 1.0d);
		renegeCondition.setOutput(true);
		this.addInput(renegeCondition);

		renegeDestination = new InterfaceEntityInput<>(Linkable.class, "RenegeDestination", KEY_INPUTS, null);
		this.addInput(renegeDestination);

		maxValidLength = new SampleInput("MaxValidLength", KEY_INPUTS, 10000);
		maxValidLength.setValidRange(0, Double.POSITIVE_INFINITY);
		maxValidLength.setIntegerValue(true);
		maxValidLength.setOutput(true);
		this.addInput(maxValidLength);

		spacing = new SampleInput("Spacing", FORMAT, 0.0d);
		spacing.setUnitType(DistanceUnit.class);
		spacing.setOutput(true);
		this.addInput(spacing);

		maxPerLine = new SampleInput("MaxPerLine", FORMAT, Double.POSITIVE_INFINITY);
		maxPerLine.setValidRange(1, Double.POSITIVE_INFINITY);
		maxPerLine.setIntegerValue(true);
		maxPerLine.setOutput(true);
		this.addInput(maxPerLine);

		maxRows = new SampleInput("MaxRows", FORMAT, Double.POSITIVE_INFINITY);
		maxRows.setValidRange(1, Double.POSITIVE_INFINITY);
		maxRows.setIntegerValue(true);
		maxRows.setOutput(true);
		this.addInput(maxRows);

		showEntities = new BooleanProvInput("ShowEntities", FORMAT, true);
		this.addInput(showEntities);
	}

	public Queue() {
		storage = new EntStorage();
		userList = new ArrayList<>();
		stats = new TimeBasedStatistics();
		freq = new TimeBasedFrequency(0, 10);
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Queue)ent).updateRenegeTimeCallback();
		}
	};

	void updateRenegeTimeCallback() {
		boolean bool = !renegeTime.isDefault();
		renegeDestination.setRequired(bool);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		storage.clear();

		// Clear statistics
		stats.clear();
		stats.addValue(0.0d, 0.0d);
		freq.clear();
		freq.addValue(0.0d, 0);
		numberReneged = 0;

		// Identify the objects that use this queue
		userList.clear();
		for (Entity each : getJaamSimModel().getClonesOfIterator(Entity.class, QueueUser.class)) {
			QueueUser u = (QueueUser) each;
			if (u.getQueues().contains(this))
				userList.add(u);
		}
	}

	public boolean isFIFO(double simTime) {
		return fifo.getNextBoolean(this, simTime);
	}

	public boolean isShowEntities(double simTime) {
		return showEntities.getNextBoolean(this, simTime);
	}

	private static class QueueEntry extends EntStorage.StorageEntry {
		final EventHandle renegeHandle;

		public QueueEntry(DisplayEntity ent, String m, int pri, long n, double t, EventHandle rh) {
			super(ent, m, pri, n, t);
			renegeHandle = rh;
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
		double simTime = getSimTime();

		// Update the queue statistics
		stats.addValue(simTime, storage.size() + 1);
		freq.addValue(simTime, storage.size() + 1);

		// Build the entry for the entity
		long n = this.getTotalNumberAdded();
		if (!isFIFO(simTime))
			n *= -1;
		int pri = (int) priority.getNextSample(this, simTime);
		String m = null;
		if (!match.isDefault())
			m = match.getNextString(this, simTime, 1.0d, true);

		EventHandle rh = null;
		if (!renegeTime.isDefault())
			rh = new EventHandle();

		QueueEntry entry = new QueueEntry(ent, m, pri, n, simTime, rh);
		storage.add(entry);

		int maxLength = (int) maxValidLength.getNextSample(this, simTime);
		if (storage.size() > maxLength)
			error("Number of objects in the queue exceeds the limit of %s set by the "
					+ "'MaxValidLength' input.", maxLength);

		// Notify the users of this queue
		if (!userUpdateHandle.isScheduled())
			EventManager.scheduleTicks(0, 2, false, userUpdate, userUpdateHandle);

		// Schedule the time to check the renege condition
		if (!renegeTime.isDefault()) {
			double dur = renegeTime.getNextSample(this, getSimTime());
			// Schedule the renege tests in FIFO order so that if two or more entities are added to
			// the queue at the same time, the one nearest the front of the queue is tested first
			EventManager.scheduleSeconds(dur, 5, true, new RenegeActionTarget(this, entry), rh);
		}
	}

	private static class RenegeActionTarget extends EntityTarget<Queue> {
		private final QueueEntry entry;

		RenegeActionTarget(Queue q, QueueEntry e) {
			super(q, "renegeAction");
			entry = e;
		}

		@Override
		public void process() {
			ent.renegeAction(entry);
		}
	}

	public void renegeAction(QueueEntry entry) {

		// Temporarily set the obj entity to the one that might renege
		double simTime = this.getSimTime();
		DisplayEntity oldEnt = this.getReceivedEntity(simTime);
		this.setReceivedEntity(entry.entity);

		// Check the condition for reneging
		boolean bool = (renegeCondition.getNextSample(this, simTime) == 0.0d);
		this.setReceivedEntity(oldEnt);
		if (bool) {
			return;
		}

		// Remove the entity from the queue and send it to the renege destination
		this.remove(entry);
		numberReneged++;
		renegeDestination.getValue().addEntity(entry.entity);
	}

	public DisplayEntity removeEntity(DisplayEntity ent) {
		return this.remove(getQueueEntry(ent));
	}

	/**
	 * Removes a specified entity from the queue
	 */
	private DisplayEntity remove(QueueEntry entry) {
		double simTime = getSimTime();

		// Update the queue statistics
		stats.addValue(simTime, storage.size() - 1);
		freq.addValue(simTime, storage.size() - 1);

		// Remove the entity from the storage
		boolean found = storage.remove(entry);
		if (!found)
			error("Cannot find the entry in itemSet.");

		// Kill the renege event
		if (entry.renegeHandle != null)
			EventManager.killEvent(entry.renegeHandle);

		// Reset the entity's orientation to its original value
		entry.entity.setShow(true);

		// Notify any observers
		notifyObservers();

		this.releaseEntity(simTime);
		return entry.entity;
	}

	private QueueEntry getQueueEntry(DisplayEntity ent) {
		Iterator<StorageEntry> itr = storage.iterator();
		while (itr.hasNext()) {
			QueueEntry entry = (QueueEntry) itr.next();
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
		Iterator<StorageEntry> itr = storage.iterator();
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
		return removeFirst(null);
	}

	/**
	 * Returns the first entity in the queue.
	 * @return first entity in the queue.
	 */
	public DisplayEntity getFirst() {
		return getFirst(null);
	}

	/**
	 * Returns the number of entities in the queue
	 */
	public int getCount() {
		return getCount(null);
	}

	/**
	 * Returns true if the queue is empty
	 */
	public boolean isEmpty() {
		return isEmpty(null);
	}

	/**
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		QueueEntry entry = (QueueEntry) storage.first();
		return this.getSimTime() - entry.timeAdded;
	}

	/**
	 * Returns the priority value for the first object in the queue
	 */
	public int getFirstPriority() {
		return storage.first().priority;
	}

	/**
	 * Returns the number of times that the specified match value appears in
	 * the queue. If the match value is null, then every entity is counted.
	 * @param m - value to be matched.
	 * @return number of entities that have this match value.
	 */
	public int getCount(String m) {
		return storage.size(m);
	}

	public boolean isEmpty(String m) {
		return storage.isEmpty(m);
	}

	public DisplayEntity getFirst(String m) {
		StorageEntry entry = storage.first(m);
		if (entry == null)
			return null;
		return entry.entity;
	}

	public double getTimeAdded(String m) {
		return storage.first(m).timeAdded;
	}

	/**
	 * Returns the first entity in the queue whose match value is equal to the
	 * specified value. The returned entity is removed from the queue.
	 * If the match value is null, the first entity is removed.
	 * @param m - value to be matched.
	 * @return entity whose match value equals the specified value.
	 */
	public DisplayEntity removeFirst(String m) {
		QueueEntry entry = (QueueEntry) storage.first(m);
		if (entry == null)
			return null;
		return this.remove(entry);
	}

	/**
	 * Returns the first entity in the queue whose match value is equal to the specified value and
	 * that satisfies the SelectionCondition for the specified LinkedService.
	 * @param m - value to be matched
	 * @param serv - LinkedService that determines the entities that are eligible
	 * @param ent - only eligible entity (null indicates no restriction)
	 * @return selected entity
	 */
	public DisplayEntity getFirst(String m, LinkedService serv, double simTime, DisplayEntity ent) {
		QueueEntry entry = getFirstEntry(m, serv, simTime, ent);
		if (entry == null)
			return null;
		return entry.entity;
	}

	/**
	 * Removes and returns the first entity in the queue whose match value is equal to the
	 * specified value and that satisfies the SelectionCondition for the specified LinkedService.
	 * @param m - value to be matched
	 * @param serv - LinkedService that determines the entities that are eligible
	 * @param ent - only eligible entity (null indicates no restriction)
	 * @return selected entity
	 */
	public DisplayEntity removeFirst(String m, LinkedService serv, double simTime, DisplayEntity ent) {
		QueueEntry entry = getFirstEntry(m, serv, simTime, ent);
		if (entry == null)
			return null;
		return remove(entry);
	}

	private QueueEntry getFirstEntry(String m, LinkedService serv, double simTime, DisplayEntity ent) {
		Iterator<StorageEntry> itr = storage.iterator(m);
		if (itr == null)
			return null;
		while (itr.hasNext()) {
			QueueEntry entry = (QueueEntry) itr.next();
			if ((ent == null || entry.entity == ent) && serv.isAllowed(entry.entity, simTime))
				return entry;
		}
		return null;
	}

	/**
	 * Returns the match value that has the largest number of entities in the queue.
	 * @return match value with the most entities.
	 */
	public String getMatchForMax() {
		return storage.getTypeWithMaxCount();
	}

	/**
	 * Returns the number of entities in the longest match value queue.
	 * @return number of entities in the longest match value queue.
	 */
	public int getMaxCount() {
		return storage.getCountForMaxType();
	}

	/**
	 * Returns the set of entity types that are present in this Queue.
	 */
	public Set<String> getEntityTypes() {
		return storage.getTypes();
	}

	public ArrayList<DisplayEntity> getEntityList() {
		return getEntityList(null);
	}

	public ArrayList<DisplayEntity> getEntityList(String m) {
		return storage.getEntityList(m);
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		boolean visible = isShowEntities(simTime);
		Quaternion orientQ = new Quaternion();
		orientQ.setEuler3(getOrientation());
		Vec3d qSize = this.getSize();
		Vec3d tmp = new Vec3d();
		int maxPerLineVal = (int) maxPerLine.getNextSample(this, simTime);
		int maxRowsVal = (int) maxRows.getNextSample(this, simTime);

		double distanceX = 0.5d * qSize.x;
		double maxWidth = 0;
		double maxHeight = 0;

		// Copy the storage entries to avoid some concurrent modification exceptions
		ArrayList<DisplayEntity> entityList;
		try {
			entityList = storage.getEntityList(null);
		}
		catch (Exception e) {
			return;
		}

		// If the queue is not visible show the entities at the sub-model's process position
		if (!getShow() && getVisibleParent() instanceof CompoundEntity) {
			CompoundEntity ce = (CompoundEntity) getVisibleParent();
			for (DisplayEntity ent : entityList) {
				ent.moveToProcessPosition(ce, ce.getProcessPosition());
			}
			return;
		}

		// Find the maximum width and height of the entities
		if (entityList.size() >  maxPerLineVal){
			for (DisplayEntity ent : entityList) {
				maxWidth = Math.max(maxWidth, ent.getGlobalSize().y);
				maxHeight = Math.max(maxHeight, ent.getGlobalSize().z);
			 }
		}

		// update item locations
		int i = 0;
		for (DisplayEntity ent : entityList) {

			// Calculate the row and level number for the entity
			int ind = i % maxPerLineVal;
			int row = (i / maxPerLineVal) % maxRowsVal;
			int level = (i / maxPerLineVal) / maxRowsVal;

			// Reset the x-position for the first entity in a row
			if( i > 0 && ind == 0 ){
				distanceX = 0.5d * qSize.x;
			}

			i++;

			// Set the region
			ent.setRegion(this.getCurrentRegion());

			// Rotate each transporter about its center so it points to the right direction
			ent.setRelativeOrientation(orientQ);
			ent.setShow(visible);

			// Calculate the y- and z- coordinates
			double space = spacing.getNextSample(this, simTime);
			double distanceY = row * (space + maxWidth);
			double distanceZ = level * (space + maxHeight);

			// Calculate the x-coordinate
			double length = ent.getGlobalSize().x;
			distanceX += 0.5d * length;
			tmp.set3(-distanceX, distanceY, distanceZ);

			// increment total distance
			distanceX += 0.5d * length + space;

			// Set Position
			Vec3d pos = this.getGlobalPositionForPosition(tmp);
			ent.setGlobalPositionForAlignment(pos, new Vec3d());
		}
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		double simTime = this.getSimTime();
		stats.clear();
		stats.addValue(simTime, storage.size());
		freq.clear();
		freq.addValue(simTime, storage.size());
		numberReneged = 0;
	}

	@Override
	public boolean canLink(boolean dir) {
		return true;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt, boolean dir) {
		if (!(nextEnt instanceof LinkedService))
			return;

		LinkedService serv = (LinkedService)nextEnt;
		serv.addQueue(this);
	}

	// LinkDisplayable
	@Override
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = super.getDestinationEntities();
		Linkable l = renegeDestination.getValue();
		if (l != null && (l instanceof DisplayEntity)) {
			ret.add((DisplayEntity)l);
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
		return getCount();
	}

	@Output(name = "QueueList",
	 description = "The entities in the queue.",
	    sequence = 1)
	public ArrayList<DisplayEntity> getQueueList(double simTime) {
		return getEntityList();
	}

	@Output(name = "QueueTimes",
	 description = "The waiting time for each entity in the queue.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public ArrayList<Double> getQueueTimes(double simTime) {
		return storage.getStorageTimeList(simTime);
	}

	@Output(name = "PriorityValues",
	 description = "The Priority expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public ArrayList<Integer> getPriorityValues(double simTime) {
		return storage.getPriorityList();
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public ArrayList<String> getMatchValues(double simTime) {
		return storage.getTypeList();
	}

	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getQueueLengthAverage(double simTime) {
		return stats.getMean(simTime);
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getQueueLengthStandardDeviation(double simTime) {
		return stats.getStandardDeviation(simTime);
	}

	@Output(name = "QueueLengthMinimum",
	 description = "The minimum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public int getQueueLengthMinimum(double simTime) {
		return (int) stats.getMin();
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public int getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		int ret = (int) stats.getMax();
		if (ret == 1 && freq.getBinTime(simTime, 1) == 0.0d)
			return 0;
		return ret;
	}

	@Output(name = "QueueLengthTimes",
	 description = "The total time that the queue has length 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 9)
	public double[] getQueueLengthDistribution(double simTime) {
		return freq.getBinTimes(simTime, 0, freq.getMax());
	}

	@Output(name = "QueueLengthFractions",
	 description = "Fraction of total time that the queue has length 0, 1, 2, etc.",
	  reportable = true,
	    sequence = 10)
	public double[] getQueueLengthFractions(double simTime) {
		return freq.getBinFractions(simTime, 0, freq.getMax());
	}

	@Output(name = "QueueLengthCumulativeFractions",
	 description = "Fraction of total time that the queue has length less than or equal to 0, 1, "
	             + "2, etc.",
	  reportable = true,
	    sequence = 11)
	public double[] getQueueLengthCumulativeFractions(double simTime) {
		return freq.getBinCumulativeFractions(simTime, 0, freq.getMax());
	}

	@Output(name = "AverageQueueTime",
	 description = "Average time each entity waits in the queue. "
	             + "Calculated as total queue time to date divided by the total number of "
	             + "entities added to the queue.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 12)
	public double getAverageQueueTime(double simTime) {
		return stats.getSum(simTime) / getNumberAdded(simTime);
	}

	@Output(name = "MatchValueCount",
	 description = "The present number of unique match values in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 13)
	public int getMatchValueCount(double simTime) {
		return getEntityTypes().size();
	}

	@Output(name = "UniqueMatchValues",
	 description = "The list of unique Match values for the entities in the queue.",
	    sequence = 14)
	public ArrayList<String> getUniqueMatchValues(double simTime) {
		ArrayList<String> ret = new ArrayList<>(getEntityTypes());
		Collections.sort(ret, Input.uiSortOrder);
		return ret;
	}

	@Output(name = "MatchValueCountMap",
	 description = "The number of entities in the queue for each Match expression value.\n"
	             + "For example, '[Queue1].MatchValueCountMap(\"SKU1\")' returns the number of "
	             + "entities whose Match value is \"SKU1\".",
	    unitType = DimensionlessUnit.class,
	    sequence = 15)
	public LinkedHashMap<String, Integer> getMatchValueCountMap(double simTime) {
		LinkedHashMap<String, Integer> ret = new LinkedHashMap<>(getEntityTypes().size());
		for (String m : getUniqueMatchValues(simTime)) {
			ret.put(m, getCount(m));
		}
		return ret;
	}

	@Output(name = "MatchValueMap",
	 description = "Provides a list of entities in the queue for each Match expression value.\n"
	             + "For example, '[Queue1].MatchValueMap(\"SKU1\")' returns a list of entities "
	             + "whose Match value is \"SKU1\".",
	    sequence = 16)
	public LinkedHashMap<String, ArrayList<DisplayEntity>> getMatchValueMap(double simTime) {
		LinkedHashMap<String, ArrayList<DisplayEntity>> ret = new LinkedHashMap<>(getEntityTypes().size());
		for (String m : getUniqueMatchValues(simTime)) {
			ret.put(m, getEntityList(m));
		}
		return ret;
	}

	@Output(name = "NumberReneged",
	 description = "The number of entities that reneged from the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 17)
	public long getNumberReneged(double simTime) {
		return numberReneged;
	}

	@Output(name = "QueuePosition",
	 description = "The position in the queue for an entity undergoing the RenegeCondition test.\n"
	             + "First in queue = 1, second in queue = 2, etc.",
	    unitType = DimensionlessUnit.class,
	  reportable = false,
	    sequence = 18)
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
