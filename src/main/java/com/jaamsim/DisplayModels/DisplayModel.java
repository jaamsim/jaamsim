/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import com.jaamsim.Graphics.View;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.VisibilityInfo;
import com.jaamsim.units.DistanceUnit;

public abstract class DisplayModel extends Entity {
	public static final VisibilityInfo ALWAYS = new VisibilityInfo(null, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	public static final Vec3d ONES = new Vec3d(1.0d, 1.0d, 1.0d);

	private VisibilityInfo visInfo = ALWAYS;

	@Keyword(description = "The view windows on which this model will be visible. "
	                     + "If this is empty the entity is visible on all views.",
	         exampleList = {"TitleView DefaultView"})
	private final EntityListInput<View> visibleViews;

	@Keyword(description = "The distances from the camera that this display model will be visible",
	         exampleList = {"0 100 m"})
	private final ValueListInput drawRange;

	@Keyword(description = "ModelScale scales the resulting visualization by this vector. "
	                     + "Warning!! Resizing an entity with this set to a value that is not 1 "
	                     + "is very unintuitive.",
	         exampleList = {"5 5 5"})
	private final Vec3dInput modelScale;

	private static final DoubleVector defRange = new DoubleVector(2);

	static {
		defRange.add(0.0d);
		defRange.add(Double.POSITIVE_INFINITY);
	}

	{
		attributeDefinitionList.setHidden(true);
		namedExpressionInput.setHidden(true);

		visibleViews = new EntityListInput<>(View.class, "VisibleViews", GRAPHICS, null);
		visibleViews.setDefaultText("All Views");
		this.addInput(visibleViews);

		drawRange = new ValueListInput("DrawRange", GRAPHICS, defRange);
		drawRange.setUnitType(DistanceUnit.class);
		drawRange.setValidCount(2);
		drawRange.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(drawRange);

		modelScale = new Vec3dInput( "ModelScale", GRAPHICS, new Vec3d(1, 1, 1));
		modelScale.setValidRange( 0.0001, 10000);
		this.addInput( modelScale);

	}

	public DisplayModel() {}

	public abstract DisplayModelBinding getBinding(Entity ent);

	public abstract boolean canDisplayEntity(Entity ent);

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == visibleViews || in == drawRange) {
			double minDist = drawRange.getValue().get(0);
			double maxDist = drawRange.getValue().get(1);
			// It's possible for the distance to be behind the camera, yet have the object visible (distance is to center)
			// So instead use negative infinity in place of zero to never cull when close to the camera.
			if (minDist == 0.0) {
				minDist = Double.NEGATIVE_INFINITY;
			}
			visInfo = new VisibilityInfo(visibleViews.getValue(), minDist, maxDist);
		}

	}

	public VisibilityInfo getVisibilityInfo() {

		return visInfo;
	}

	public Vec3d getModelScale() {
		return modelScale.getValue();
	}
}
