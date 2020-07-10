/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class AbstractCombine extends LinkedService {

	@Keyword(description = "The service time required to perform the assembly process.",
	         exampleList = { "3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	@Keyword(description = "A list of Queue objects in which to place the arriving sub-component entities.",
	         exampleList = {"Queue1 Queue2"})
	private final EntityListInput<Queue> waitQueueList;

	@Keyword(description = "The number of entities required from each queue for the assembly process to begin. "
	                     + "The last value in the list is used if the number of queues is greater "
	                     + "than the number of values. "
	                     + "Only an integer number of entities can be assembled. "
	                     + "A decimal value will be truncated to an integer.",
	         exampleList = {"2 1", "{ 2 } { 1 }", "{ DiscreteDistribution1 } { 'this.obj.attrib1 + 1' }"})
	private final SampleListInput numberRequired;

	@Keyword(description = "If TRUE, the all entities used in the assembly process must have the same Match value. "
			+ "The match value for an entity determined by the Match keyword for each queue. The value is calculated "
			+ "when the entity first arrives at its queue.",
	         exampleList = {"TRUE"})
	private final BooleanInput matchRequired;

	{
		waitQueue.setHidden(true);
		match.setHidden(true);
		watchList.setHidden(true);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);

		waitQueueList = new EntityListInput<>(Queue.class, "WaitQueueList", KEY_INPUTS, new ArrayList<Queue>());
		waitQueueList.setRequired(true);
		this.addInput(waitQueueList);

		ArrayList<SampleProvider> def = new ArrayList<>();
		def.add(new SampleConstant(1));
		numberRequired = new SampleListInput("NumberRequired", KEY_INPUTS, def);
		numberRequired.setDimensionless(true);
		numberRequired.setUnitType(DimensionlessUnit.class);
		this.addInput(numberRequired);

		matchRequired = new BooleanInput("MatchRequired", KEY_INPUTS, false);
		this.addInput(matchRequired);
	}

	public AbstractCombine() {}

	@Override
	public void addEntity( DisplayEntity ent ) {
		error("An entity cannot be sent directly to an Assemble object. It must be sent to the appropriate queue.");
	}

	@Override
	public void addQueue(Queue que) {
		ArrayList<String> toks = new ArrayList<>();
		waitQueueList.getValueTokens(toks);
		toks.add(que.getName());
		KeywordIndex kw = new KeywordIndex(waitQueueList.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<Queue> getQueues() {
		return waitQueueList.getValue();
	}

	public int[] getNumberRequired(double simTime) {

		// Number of values enter by the user
		int[] ret = new int[waitQueueList.getListSize()];
		for (int i = 0; i < numberRequired.getListSize(); i++) {
			int n = (int) numberRequired.getValue().get(i).getNextSample(simTime);
			ret[i] = n;
		}

		// Additional copies of the last value needed to complete the list
		int lastVal = ret[numberRequired.getListSize() - 1];
		for (int i = numberRequired.getListSize(); i < waitQueueList.getListSize(); i++) {
			ret[i] = lastVal;
		}
		return ret;
	}

	public boolean isMatchRequired() {
		return matchRequired.getValue();
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceTime.getValue().getNextSample(simTime);
	}

	/**
	 * Returns a match value that has sufficient numbers of entities in each
	 * queue. The first match value that satisfies the criterion is selected.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @return match value.
	 */
	public static String selectMatchValue(ArrayList<Queue> queueList, int[] numberList) {

		// Check whether each queue has sufficient entities for any match value
		for (int i=0; i<queueList.size(); i++) {
			if (queueList.get(i).getMaxCount() < numberList[i])
				return null;
		}

		// Find the queue with the fewest match values
		Queue shortest = null;
		int count = Integer.MAX_VALUE;
		for (Queue que : queueList) {
			if (que.getEntityTypes().size() < count) {
				count = que.getEntityTypes().size();
				shortest = que;
			}
		}

		// Find the match values that have sufficient entities in each queue
		ArrayList<String> matchList = new ArrayList<>();
		for (String m : shortest.getEntityTypes()) {
			if (sufficientEntities(queueList, numberList, m))
				matchList.add(m);
		}

		// Select the match value with the earliest entity arrival
		String ret = null;
		double earliestTime = Double.POSITIVE_INFINITY;
		for (String m : matchList) {
			for (Queue que : queueList) {
				double timeAdded = que.getTimeAddedForMatch(m);
				if (timeAdded < earliestTime) {
					ret = m;
					earliestTime = timeAdded;
				}
			}
		}
		return ret;
	}

	/**
	 * Returns true if each of the queues contains sufficient entities with
	 * the specified match value for processing to begin.
	 * If the match value m is null, then all the entities in each queue are counted.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @param m - match value.
	 * @return true if there are sufficient entities in each queue.
	 */
	public static boolean sufficientEntities(ArrayList<Queue> queueList, int[] numberList, String m) {
		for (int i = 0; i < queueList.size(); i++) {
			if (queueList.get(i).getMatchCount(m) < numberList[i])
				return false;
		}
		return true;
	}

}
