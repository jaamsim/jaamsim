/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
