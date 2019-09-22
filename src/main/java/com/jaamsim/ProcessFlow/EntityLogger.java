/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015-2019 JaamSim Software Inc.
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
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LinkDisplayable;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.math.Vec3d;

public class EntityLogger extends Logger implements Linkable, LinkDisplayable {

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
			exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	{
		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);
	}

	private DisplayEntity receivedEntity;

	public EntityLogger() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		receivedEntity = null;
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		receivedEntity = ent;

		// Record the entry in the log
		this.recordLogEntry(getSimTime());

		// Send the entity to the next element in the chain
		nextComponent.getValue().addEntity(ent);
	}

	@Override
	protected void printColumnTitles(FileEntity file) {
		file.format("\t%s", "this.obj");
	}

	@Override
	protected void recordEntry(FileEntity file, double simTime) {
		file.format("\t%s", receivedEntity);
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (nextComponent.getHidden() || !(nextEnt instanceof Linkable)
				|| nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	// LinkDisplayable
	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = new ArrayList<>();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof Entity)) {
			ret.add((Entity)l);
		}
		return ret;
	}

	@Override
	public ArrayList<Entity> getSourceEntities() {
		return new ArrayList<>();
	}

	@Override
	public Vec3d getSourcePoint() {
		return getGlobalPosition();
	}

	@Override
	public Vec3d getSinkPoint() {
		return getGlobalPosition();
	}

	@Override
	public double getRadius() {
		return getSize().mag2()/2.0;
	}

	@Output(name = "obj",
	 description = "The entity that was received most recently.",
	    sequence = 0)
	public DisplayEntity getReceivedEntity(double simTime) {
		return receivedEntity;
	}

}
