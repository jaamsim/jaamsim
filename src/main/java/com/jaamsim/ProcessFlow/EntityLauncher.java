/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LinkDisplayable;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringInput;
import com.jaamsim.math.Vec3d;
import com.jogamp.newt.event.KeyEvent;

public class EntityLauncher extends DisplayEntity implements LinkDisplayable {

	@Keyword(description = "The prototype for entities to be generated. "
	                     + "The generated entities will be copies of this entity.",
	         exampleList = {"Proto", "'choose( this.NumberGenerated%2+1, [Proto1], [Proto2])'"})
	private final EntityProvInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The object to which the generated DisplayEntity is passed.",
	         exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	@Keyword(description = "",
	         exampleList = {""})
	private final StringInput actionKey;

	private EventManager evt;
	private int numberGenerated = 0;  // Number of entities generated so far

	{
		prototypeEntity = new EntityProvInput<>(DisplayEntity.class, "PrototypeEntity", "Key Inputs", null);
		prototypeEntity.setEntity(this);
		prototypeEntity.setRequired(true);
		prototypeEntity.addInvalidClass(TextBasics.class);
		prototypeEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(prototypeEntity);

		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", "Key Inputs", null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		actionKey = new StringInput("ActionKey", "Key Inputs", null);
		this.addInput(actionKey);
	}

	public EntityLauncher() {}
	
	@Override
	public void earlyInit() {
		super.earlyInit();
		evt = null;
	}
	
	@Override
	public void startUp() {
		super.startUp();
		evt = EventManager.current();
	}

	@Override
	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		super.handleKeyReleased(keyCode, keyChar, shift, control, alt);

		// Space bar generates a new entity
		if (keyCode == KeyEvent.VK_SPACE) {
			generateEntity();
			return;
		}
	}

	@Override
	public void handleMouseClicked(short count, Vec3d globalCoord) {

		// Double click generates a new entity
		if (count == 2) {
			generateEntity();
			return;
		}
	}

	private void generateEntity() {
		if (evt == null || doActionHandle.isScheduled())
			return;
		evt.scheduleProcessExternal(0L, 0, false, doActionTarget, doActionHandle);
	}

	public void doAction() {

		// Create a new entity
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue().getNextEntity(0.0d);
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName()).append("_").append(numberGenerated);
		DisplayEntity ent = InputAgent.generateEntityWithName(proto.getClass(), sb.toString());
		Entity.fastCopyInputs(proto, ent);
		ent.earlyInit();

		// Send the entity to the next element in the chain
		nextComponent.getValue().addEntity(ent);
	}

	/**
	 * DoActionTarget
	 */
	private static class DoActionTarget extends EntityTarget<EntityLauncher> {
		DoActionTarget(EntityLauncher ent) {
			super(ent, "doAction");
		}

		@Override
		public void process() {
			ent.doAction();
		}
	}
	private final ProcessTarget doActionTarget = new DoActionTarget(this);
	private final EventHandle doActionHandle = new EventHandle();

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (nextComponent.getHidden() || !(nextEnt instanceof Linkable)
				|| nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.apply(this, kw);
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

}
