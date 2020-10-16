/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
