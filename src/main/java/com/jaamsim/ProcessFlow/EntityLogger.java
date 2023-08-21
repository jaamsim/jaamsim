/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015-2023 JaamSim Software Inc.
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

import com.jaamsim.BasicObjects.Logger;
import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateEntityListener;
import com.jaamsim.states.StateRecord;

public class EntityLogger extends Logger implements Linkable, StateEntityListener {

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
			exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	@Keyword(description = "If TRUE, an entry will made in the log file every time one of the "
	                     + "received entities changes state.")
	private final BooleanProvInput traceEntityStates;

	{
		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		traceEntityStates = new BooleanProvInput("TraceEntityStates", KEY_INPUTS, false);
		this.addInput(traceEntityStates);
	}

	private DisplayEntity receivedEntity;

	public EntityLogger() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		receivedEntity = null;
	}

	public boolean isTraceEntityStates(double simTime) {
		return traceEntityStates.getNextBoolean(this, simTime);
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		receivedEntity = ent;

		// Trace states for received entities
		double simTime = getSimTime();
		if (isTraceEntityStates(simTime) && ent instanceof StateEntity) {
			((StateEntity) ent).addStateListener(this);
			nextComponent.getValue().addEntity(ent);
			return;
		}

		// Record the entry in the log
		this.recordLogEntry(getSimTime(), ent);

		// Send the entity to the next element in the chain
		nextComponent.getValue().addEntity(ent);
	}

	@Override
	protected void printColumnTitles(FileEntity file) {
		double simTime = getSimTime();
		if (isTraceEntityStates(simTime)) {
			file.format("\t%s\t%s", "Entity", "State");
			return;
		}
		file.format("\t%s", "this.obj");
	}

	@Override
	protected void recordEntry(FileEntity file, double simTime, DisplayEntity ent) {
		if (isTraceEntityStates(simTime) && ent instanceof StateEntity) {
			file.format("\t%s\t%s", ent, ((StateEntity) ent).getPresentState(simTime));
			return;
		}
		file.format("\t%s", ent);
	}

	@Override
	public boolean isWatching(StateEntity ent) {
		// Method not used
		return false;
	}

	@Override
	public void updateForStateChange(StateEntity ent, StateRecord prev, StateRecord next) {
		recordLogEntry(getSimTime(), ent);
	}

	@Override
	public boolean canLink(boolean dir) {
		return !nextComponent.getHidden();
	}

	@Override
	public void linkTo(DisplayEntity nextEnt, boolean dir) {
		if (!(nextEnt instanceof Linkable) || nextEnt instanceof EntityGen)
			return;

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = super.getDestinationEntities();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof DisplayEntity)) {
			ret.add((DisplayEntity)l);
		}
		return ret;
	}

	@Output(name = "obj",
	 description = "The entity that was received most recently.",
	    sequence = 0)
	public DisplayEntity getReceivedEntity(double simTime) {
		return receivedEntity;
	}

}
