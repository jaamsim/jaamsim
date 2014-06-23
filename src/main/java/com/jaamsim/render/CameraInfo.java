/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.net.URI;

import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Transform;

/**
 * The basic configuration information for a camera. This class is a simple data
 * container for Camera configuration, but not the rendering specific methods that would be needed
 * in an actual camera. This Class can cross safely into Application space (unlike an actual Camera)
 * @author Matt.Chudleigh
 *
 */
public class CameraInfo {

	public double FOV;
	public Transform trans;
	public URI skyboxTexture;

	public CameraInfo(CameraInfo ci) {
		this.FOV = ci.FOV;
		this.skyboxTexture = ci.skyboxTexture;

		this.trans = new Transform(ci.trans);
	}

	public CameraInfo(double FOV, Transform transRef, URI skyboxTexture) {
		this.FOV = FOV;
		this.skyboxTexture = skyboxTexture;

		this.trans = new Transform(transRef);
	}

	/**
	 * Very similar to equals() but not
	 * @param other
	 * @return
	 */
	public boolean isSame(CameraInfo other) {
		boolean isSame = true;
		isSame = isSame && MathUtils.near(other.FOV, FOV);
		isSame = isSame && other.trans.equals(trans);

		if (other.skyboxTexture != null)
			isSame = isSame && other.skyboxTexture.equals(skyboxTexture);
		else if (skyboxTexture != null) // Handle other.skyboxTexture == null, but this.skyboxTexture != null
			isSame = false;

		return isSame;
	}
}
