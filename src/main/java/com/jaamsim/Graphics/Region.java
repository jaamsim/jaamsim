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
package com.jaamsim.Graphics;

import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;

public class Region extends DisplayEntity {

	{
		this.addSynonym(positionInput, "Origin");

		desc.setHidden(true);
		sizeInput.setHidden(true);
		alignmentInput.setHidden(true);
		regionInput.setHidden(true);
		relativeEntity.setHidden(true);
		displayModelListInput.setHidden(true);
	}

	public Region() {}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * point to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTrans() {
		Quaternion rot = new Quaternion();
		rot.setEuler3(getOrientation());
		return new Transform(getPosition(), rot, 1.0d);
	}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * vector to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTransForVectors() {
		Quaternion rot = new Quaternion();
		rot.setEuler3(getOrientation());
		return new Transform(null, rot, 1.0d);
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
