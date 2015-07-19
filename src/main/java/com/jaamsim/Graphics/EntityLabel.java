/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.ui.FrameBox;
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
			setSavedText(targetEntity.getValue().getName());
			return;
		}
	}

	@Override
	public void acceptEdits() {
		try {
			// Rename both the target entity and the label
			String newName = getEditText();
			InputAgent.renameEntity(targetEntity.getValue(), newName);
			InputAgent.renameEntity(this, newName + "_Label");

			// Update the entries in the Object Selector
			ObjectSelector.allowUpdate();
			FrameBox.reSelectEntity();

			super.acceptEdits();
		}
		catch (ErrorException e) {
			super.cancelEdits();
		}
	}

	private DisplayEntity getTarget() {
		return targetEntity.getValue();
	}

}
