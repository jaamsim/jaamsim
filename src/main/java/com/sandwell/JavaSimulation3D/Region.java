/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Vec3dInput;

/**
 * Entity which defines its own locale to add branch groups to. Analogous to
 * stage construct. Abstract class as it does not define the doProcess() method.
 */
public class Region extends Entity {
@Keyword(desc = "The location of the origin (0,0,0) of the region in the world.",
      example = "Region1 Origin { -3.922 -1.830 0.000 m }")
private final Vec3dInput originInput;

@Keyword(desc = "Euler angles describing the orientation of the region's local coordinate system.",
      example = "Region1 Orientation { 0 0 90 deg }")
private final Vec3dInput orientationInput;

	private final ChangeWatcher graphicsDirtier = new ChangeWatcher();

{
	originInput = new Vec3dInput("Origin", "Basic Graphics", null);
	originInput.setUnits("m");
	this.addInput(originInput, true);

	orientationInput = new Vec3dInput("Orientation", "Basic Graphics", null);
	orientationInput.setUnits("rad");
	this.addInput(orientationInput, true);
}

	/**
	 * Constructor creating a new locale in the simulation universe.
	 */
	public Region() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == originInput ||
			in == orientationInput) {
			graphicsDirtier.changed();
		}
	}

	public ChangeWatcher getGraphicsDirtier() {
		return graphicsDirtier;
	}

	public Transform getRegionTrans(double simTime) {
		Quaternion rot = null;
		Vec3d temp = orientationInput.getValue();
		if (temp != null)
			rot = Quaternion.FromEuler(temp.x, temp.y, temp.z);

		temp = originInput.getValue();
		return new Transform(temp, rot, 1.0d);
	}
}
