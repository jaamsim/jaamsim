/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.View;
import com.jaamsim.units.DistanceUnit;

public class EntityLabel extends TextBasics {

	@Keyword(description = "The name of an entity that is labelled by this EntityLabel.",
	         exampleList = {"DisplayEntity1"})
	protected final EntityInput<DisplayEntity> targetEntity;

	{
		attributeDefinitionList.setHidden(true);
		namedExpressionInput.setHidden(true);

		targetEntity = new EntityInput<>(DisplayEntity.class, "TargetEntity", KEY_INPUTS, null);
		this.addInput(targetEntity);

		targetEntity.setHidden(true);
		relativeEntity.setHidden(true);
		regionInput.setHidden(true);
	}

	public EntityLabel() {}

	public static EntityLabel getLabel(DisplayEntity ent) {
		for (EntityLabel label : ent.getJaamSimModel().getClonesOfIterator(EntityLabel.class)) {
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
				setText("ERROR");
				return;
			}
			setText(ent.getName());
			return;
		}

		if (in == textHeight) {
			resizeForText();
			return;
		}
	}

	@Override
	public void acceptEdits() {
		try {
			// Rename both the target entity and the label
			InputAgent.renameEntity(targetEntity.getValue(), getText());
			super.acceptEdits();
		}
		catch (ErrorException e) {
			super.cancelEdits();
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.invokeErrorDialogBox("Input Error", e.getMessage());
		}
	}

	private DisplayEntity getTarget() {
		return targetEntity.getValue();
	}

	public void updateForTargetNameChange() {
		String targetName = targetEntity.getValue().getName();
		setText(targetName);
		this.resizeForText();
	}

	public static EntityLabel createLabel(DisplayEntity ent) {
		EntityLabel label = getLabel(ent);
		if (label != null)
			return label;

		// Create the EntityLabel object
		JaamSimModel simModel = ent.getJaamSimModel();
		String name = InputAgent.getUniqueName(simModel, ent.getName(), "_Label");
		InputAgent.storeAndExecute(new DefineCommand(simModel, EntityLabel.class, name));
		label = (EntityLabel)simModel.getNamedEntity(name);

		// Assign inputs that link the label to its target entity
		InputAgent.applyArgs(label, "TargetEntity", ent.getName());
		InputAgent.applyArgs(label, "RelativeEntity", ent.getName());
		if (ent.getCurrentRegion() != null)
			InputAgent.applyArgs(label, "Region", ent.getCurrentRegion().getName());

		// Set the visible views to match its target entity
		if (ent.getVisibleViews() != null) {
			ArrayList<String> tokens = new ArrayList<>(ent.getVisibleViews().size());
			for (View v : ent.getVisibleViews()) {
				tokens.add(v.getName());
			}
			KeywordIndex kw = new KeywordIndex("VisibleViews", tokens, null);
			InputAgent.apply(label, kw);
		}

		// Set the label's position
		Vec3d pos = getNominalPosition(ent);
		InputAgent.apply(label, InputAgent.formatVec3dInput("Position", pos, DistanceUnit.class));

		// Set the label's size
		label.resizeForText();

		return label;
	}

	public static Vec3d getNominalPosition(DisplayEntity ent) {
		double ypos = -0.15d - 0.5d*ent.getSize().y;
		return new Vec3d(0.0d, ypos, 0.0d);
	}

	public static void showLabel(DisplayEntity ent, boolean bool) {
		EntityLabel label = getLabel(ent);

		// Does the label exist yet?
		if (label == null) {
			if (!bool)
				return;
			label = EntityLabel.createLabel(ent);
		}

		// Show or hide the label
		if (label.getShow() == bool)
			return;
		InputAgent.applyBoolean(label, "Show", bool);
	}

}
