/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import com.jaamsim.Commands.CoordinateCommand;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.units.DimensionlessUnit;

public class Region extends DisplayEntity {

	@Keyword(description = "If TRUE, the object is displayed in the View windows.",
	         exampleList = {"FALSE"})
	private final ValueInput scale;

	{
		this.addSynonym(positionInput, "Origin");

		desc.setHidden(true);

		scale = new ValueInput("Scale", KEY_INPUTS, 1.0d);
		scale.setUnitType(DimensionlessUnit.class);
		this.addInput(scale);
	}

	public Region() {}

	@Override
	public void setInputsForDragAndDrop() {}

	public double getScale() {
		return scale.getValue();
	}

	@Override
	public void delete() {

		// Reset the Region input for the entities in this region
		KeywordIndex kw = InputAgent.formatArgs("Region");
		for (DisplayEntity ent : getJaamSimModel().getClonesOfIterator(DisplayEntity.class)) {
			if (ent == this || ent.getInput("Region").getValue() != this)
				continue;
			InputAgent.storeAndExecute(new CoordinateCommand(ent, kw));
		}

		super.delete();
	}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * point to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTrans() {
		Quaternion rot = new Quaternion();
		rot.setEuler3(getOrientation());
		return new Transform(getGlobalPosition(), rot, getScale());
	}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * vector to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTransForVectors() {
		Quaternion rot = new Quaternion();
		rot.setEuler3(getOrientation());
		return new Transform(null, rot, getScale());
	}

	/**
	 * Return the transformation that converts the global coordinates for a
	 * point to local coordinates for the region.
	 * @return transformation to global coordinates
	 */
	public Transform getInverseRegionTrans() {
		Transform trans = new Transform();
		this.getRegionTrans().inverse(trans);
		return trans;
	}

	/**
	 * Return the transformation that converts the global coordinates for a
	 * vector to local coordinates for the region.
	 * @return transformation to global coordinates
	 */
	public Transform getInverseRegionTransForVectors() {
		Transform trans = new Transform();
		this.getRegionTransForVectors().inverse(trans);
		return trans;
	}

}
