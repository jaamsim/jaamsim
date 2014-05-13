/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.VisibilityInfo;
import com.jaamsim.ui.View;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public abstract class DisplayModel extends Entity {
	public static final VisibilityInfo ALWAYS = new VisibilityInfo(null, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	public static final Vec3d ONES = new Vec3d(1.0d, 1.0d, 1.0d);

	private VisibilityInfo visInfo = ALWAYS;

	@Keyword(description = "The view objects this model will be visible on. If this is empty the entity is visible on all views.",
	         example = "ShipModel VisibleViews { TitleView DefaultView }")
	private final EntityListInput<View> visibleViews;

	@Keyword(description = "The distances from the camera that this display model will be visible",
	         example = "ShipModel DrawRange { 0 100 m }")
	private final ValueListInput drawRange;

	@Keyword(description = "ModelScale scales the resulting visualization by this vector. Warning!! Resizing an entity with this set " +
	         "to a value that is not 1 is very unintuitive.",
	         example = "ShipModel ModelScale { 5 5 5 }")
	private final Vec3dInput modelScale;

	private static final DoubleVector defRange = new DoubleVector(2);

	static {
		defRange.add(0.0d);
		defRange.add(Double.POSITIVE_INFINITY);
	}

	{
		visibleViews = new EntityListInput<View>(View.class, "VisibleViews", "Basic Graphics", null);
		this.addInput(visibleViews);

		drawRange = new ValueListInput("DrawRange", "Basic Graphics", defRange);
		drawRange.setUnitType(DistanceUnit.class);
		drawRange.setValidCount(2);
		drawRange.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(drawRange);

		modelScale = new Vec3dInput( "ModelScale", "Basic Graphics", new Vec3d(1, 1, 1));
		modelScale.setValidRange( 0.0001, 10000);
		this.addInput( modelScale);

	}

	public DisplayModel() {}

	public abstract DisplayModelBinding getBinding(Entity ent);

	public abstract boolean canDisplayEntity(Entity ent);

	public static DisplayModel getDefaultDisplayModelForClass(Class<? extends DisplayEntity> theClass) {
		for( ObjectType type : ObjectType.getAll() ) {
			if(type.getJavaClass() == theClass) {
				return type.getDefaultDisplayModel();
			}
		}
		return null;
	}

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
