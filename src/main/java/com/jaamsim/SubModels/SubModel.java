/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2022 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.ui.DragAndDropable;

public class SubModel extends AbstractSubModel implements DragAndDropable {

	@Keyword(description = "Defines new keywords for the sub-model and creates new outputs with "
	                     + "the same names. "
	                     + "This allows the components of a sub-model to receive all their inputs "
	                     + "from either the parent sub-model or from other components.",
	         exampleList = {"{ ServiceTime TimeUnit } { NumberOfUnits }"})
	protected final PassThroughListInput keywordListInput;

	public static final String PALETTE_NAME = "Pre-built SubModels";

	{
		keywordListInput = new PassThroughListInput("KeywordList", OPTIONS, new ArrayList<PassThroughData>());
		keywordListInput.setCallback(keywordListKeywordCallback);
		this.addInput(keywordListInput);
	}

	public SubModel() {
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	static final InputCallback keywordListKeywordCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			SubModel sm = (SubModel)ent;
			PassThroughListInput ptl = (PassThroughListInput)inp;

			sm.updateKeywords(ptl.getValue());
			for (SubModelClone clone : sm.getClones()) {
				clone.updateKeywords(ptl.getValue());
			}
			GUIListener gui = sm.getJaamSimModel().getGUIListener();
			if (gui != null && gui.isSelected(sm))
				gui.updateInputEditor();
		}
	};

	@Override
	public void kill() {
		super.kill();
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	@Override
	public void restore(String name) {
		super.restore(name);
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	@Override
	public void validate() {
		// If there are clones, only the clones need to be validated
		if (hasClone())
			return;
		super.validate();
	}

	public void updateClones() {
		for (SubModelClone clone : getClones()) {
			clone.update();
		}
	}

	/**
	 * Returns the clones that were made from this prototype sub-model.
	 * @return list of clones of this prototype.
	 */
	public ArrayList<SubModelClone> getClones() {
		ArrayList<SubModelClone> ret = new ArrayList<>();
		JaamSimModel simModel = getJaamSimModel();
		for (SubModelClone clone : simModel.getClonesOfIterator(SubModelClone.class)) {
			if (clone.isClone(this)) {
				ret.add(clone);
			}
		}
		return ret;
	}

	public boolean hasClone() {
		JaamSimModel simModel = getJaamSimModel();
		for (SubModelClone clone : simModel.getClonesOfIterator(SubModelClone.class)) {
			if (clone.isClone(this)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Class<? extends Entity> getJavaClass() {
		return SubModelClone.class;
	}

	@Override
	public Entity getPrototype() {
		return this;
	}

	@Override
	public boolean isDragAndDrop() {
		return true;
	}

	@Override
	public String getPaletteName() {
		return PALETTE_NAME;
	}

	@Override
	public BufferedImage getIconImage() {
		return getObjectType().getIconImage();
	}

}
