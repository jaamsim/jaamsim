/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2021 JaamSim Software Inc.
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
package com.jaamsim.GameObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.KeyEventInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;

public abstract class GameEntity extends DisplayEntity {

	@Keyword(description = "An optional keyboard key that will cause the entity to perform its action.",
	         exampleList = {"A", "F1", "ESCAPE"})
	private final KeyEventInput actionKey;

	{
		actionKey = new KeyEventInput("ActionKey", KEY_INPUTS, null);
		actionKey.setDefaultValue("SPACE");
		this.addInput(actionKey);
	}

	public GameEntity() {}

	@Override
	public boolean handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		// Detect the specified key event
		if (actionKey.getValue() != null && keyCode == actionKey.getValue().intValue()) {
			scheduleAction();
			return true;
		}

		// Otherwise perform the normal action for the key
		boolean ret = super.handleKeyPressed(keyCode, keyChar, shift, control, alt);
		return ret;
	}

	@Override
	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		// Detect the selected key event
		if (actionKey.getValue() != null && keyCode == actionKey.getValue().intValue()) {
			return;
		}

		// Otherwise perform the normal action for the key
		super.handleKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void handleMouseClicked(short count, Vec3d globalCoord,
			boolean shift, boolean control, boolean alt) {
		super.handleMouseClicked(count, globalCoord, shift, control, alt);

		// Single click performs the action
		if (count == 1) {
			scheduleAction();
			return;
		}
	}

	private void scheduleAction() {
		setState();
		EventManager evt = getJaamSimModel().getEventManager();
		if (evt == null || doActionHandle.isScheduled() || !getSimulation().isRealTime())
			return;
		evt.scheduleProcessExternal(0L, 0, false, doActionTarget, doActionHandle);
	}

	/**
	 * Performs any actions to occur immediately after the object is clicked, prior to any events.
	 */
	public void setState() {}

	/**
	 * Performs any actions to occur after the event that is scheduled when the object is clicked.
	 */
	public abstract void doAction();

	/**
	 * DoActionTarget
	 */
	private static class DoActionTarget extends EntityTarget<GameEntity> {
		DoActionTarget(GameEntity ent) {
			super(ent, "doAction");
		}

		@Override
		public void process() {
			ent.doAction();
		}
	}
	private final ProcessTarget doActionTarget = new DoActionTarget(this);
	private final EventHandle doActionHandle = new EventHandle();

}
