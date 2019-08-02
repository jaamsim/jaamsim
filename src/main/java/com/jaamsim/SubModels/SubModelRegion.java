/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.SubModels;

import java.util.ArrayList;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.Graphics.Region;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class SubModelRegion extends Region {

	private CompoundEntity subModel;

	{
		alignmentInput.setDefaultValue(new Vec3d());
		sizeInput.setDefaultValue(new Vec3d(1.0d, 1.0d, 0.0d));
		ArrayList<DisplayModel> def = new ArrayList<>(1);
		def.add((DisplayModel) getJaamSimModel().getNamedEntity("RegionRectangle"));
		displayModelListInput.setDefaultValue(def);

		positionInput.setHidden(true);
		sizeInput.setHidden(true);
		relativeEntity.setHidden(true);
		alignmentInput.setHidden(true);
		orientationInput.setHidden(true);
		scaleInput.setHidden(true);
	}

	public SubModelRegion() {}

	public void setSubModel(CompoundEntity sm) {
		subModel = sm;
		InputAgent.applyArgs(this, "RelativeEntity", subModel.getName());
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == positionInput) {
			Vec3d pos = positionInput.getValue();
			if (subModel != null)
				InputAgent.applyVec3d(subModel, "RegionPosition", pos, DistanceUnit.class);
			return;
		}

		if (in == sizeInput) {
			Vec3d size = getInternalSize();
			if (subModel != null)
				InputAgent.applyVec3d(subModel, "RegionSize", size, DistanceUnit.class);
			return;
		}
	}

	@Override
	public boolean isGraphicsNominal() {
		return true;
	}

}
