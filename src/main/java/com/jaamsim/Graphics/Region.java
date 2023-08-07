/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2023 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;

public class Region extends DisplayEntity {

	@Keyword(description = "The graphical scale for the Region's local coordinate system relative "
	                     + "to the coordinate system in which it is embedded. "
	                     + "For example, an input of 0.5 would make objects appear to be one-half "
	                     + "smaller and closer together.",
	         exampleList = {"0.5"})
	protected final SampleInput scaleInput;

	private double scale = 1.0d;

	{
		this.addSynonym(positionInput, "Origin");

		desc.setHidden(true);

		scaleInput = new SampleInput("Scale", KEY_INPUTS, 1.0d);
		scaleInput.setUnitType(DimensionlessUnit.class);
		scaleInput.setCallback(inputCallback);
		this.addInput(scaleInput);
	}

	public Region() {}

	@Override
	public void setInputsForDragAndDrop() {}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Region)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		setScale(scaleInput.getNextSample(this, 0.0d));
	}

	@Override
	public void resetGraphics() {
		super.resetGraphics();
		setScale(scaleInput.getNextSample(this, 0.0d));
	}

	public void setScale(double val) {
		synchronized (scaleInput) {
			scale = val;
		}
	}

	public double getScale() {
		synchronized (scaleInput) {
			return scale;
		}
	}

	public double getGlobalScale() {
		double ret = getScale();
		if (getCurrentRegion() != null)
			ret *= getCurrentRegion().getGlobalScale();
		return ret;
	}

	public Quaternion getGlobalRotation() {
		Quaternion ret = new Quaternion();
		ret.setEuler3(getOrientation());
		if (getCurrentRegion() == null)
			return ret;
		ret.mult(ret, getCurrentRegion().getGlobalRotation());
		return ret;
	}

	public Vec3d getInternalSize() {
		Vec3d ret = getSize();
		ret.scale3(1.0d/getScale());
		return ret;
	}

	/**
	 * Sets the scale factor and internal dimensions for the region.
	 * @param scale - ratio between external and internal coordinates
	 * @param internalSize - size of the region measured in its internal coordinate system
	 */
	public void setScaleAndSize(double scale, Vec3d internalSize) {
		setScale(scale);
		Vec3d size = new Vec3d(internalSize);
		size.scale3(scale);
		setSize(size);
	}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * point to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTrans() {
		return new Transform(getGlobalPosition(), getGlobalRotation(), getGlobalScale());
	}

	/**
	 * Return the transformation that converts the local coordinates for a
	 * vector to global coordinates.
	 * @return transformation to global coordinates
	 */
	public Transform getRegionTransForVectors() {
		return new Transform(null, getGlobalRotation(), getGlobalScale());
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
