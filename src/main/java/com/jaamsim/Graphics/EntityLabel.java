/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.ObjectSelector;

public class EntityLabel extends TextBasics {

	@Keyword(description = "The name of an entity that is labelled by this EntityLabel.",
	         exampleList = {"DisplayEntity1"})
	protected final EntityInput<DisplayEntity> targetEntity;

	{
		targetEntity = new EntityInput<>(DisplayEntity.class, "TargetEntity", "Key Inputs", null);
		this.addInput(targetEntity);

		targetEntity.setHidden(true);
		relativeEntity.setHidden(true);
		regionInput.setHidden(true);
	}

	public EntityLabel() {}

	public static EntityLabel getLabel(DisplayEntity ent) {
		for (EntityLabel label : Entity.getClonesOf(EntityLabel.class)) {
			if (label.getTarget() == ent)
				return label;
		}
		return null;
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == targetEntity) {
			DisplayEntity ent = targetEntity.getValue();
			if (ent == null) {
				setSavedText("ERROR");
				return;
			}
			setSavedText(ent.getName());
			return;
		}
	}

	@Override
	public void acceptEdits() {
		try {
			// Rename both the target entity and the label
			InputAgent.renameEntity(targetEntity.getValue(), getEditText());
			this.updateForTargetNameChange();

			// Update the entries in the Object Selector
			ObjectSelector.allowUpdate();
			FrameBox.reSelectEntity();

			super.acceptEdits();
		}
		catch (ErrorException e) {
			super.cancelEdits();
			GUIFrame.invokeErrorDialog("Input Error", e.getMessage());
		}
	}

	private DisplayEntity getTarget() {
		return targetEntity.getValue();
	}

	public void updateForTargetNameChange() {
		String targetName = targetEntity.getValue().getName();
		InputAgent.renameEntity(this, targetName + "_Label");
		setSavedText(targetName);
		this.resizeForText();
	}

}
