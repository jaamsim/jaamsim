/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.basicsim;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ClassInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ImageInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class ObjectType extends Entity {

	@Keyword(description = "The java class of the object type",
	         exampleList = {"This is placeholder example text"})
	private final ClassInput javaClass;

	@Keyword(description = "The package to which the object type belongs",
	         exampleList = {"This is placeholder example text"})
	private final StringInput palette;

	@Keyword(description = "Only for DisplayEntity",
	         exampleList = {"This is placeholder example text"})
	private final EntityInput<DisplayModel> defaultDisplayModel;

	@Keyword(description = "This is placeholder description text",
	         exampleList = {"This is placeholder example text"})
	private final BooleanInput dragAndDrop;

	@Keyword(description = "The (optional) image to be used in the Model Builder as the icon for "
	                     + "this object type.  The normal image size is 24x24 pixels.",
	         exampleList = {"This is placeholder example text"})
	private final ImageInput iconFile;

	@Keyword(description = "The default size for the instances of this class.",
	         exampleList = {"1.0 1.0 1.0 m"})
	private final Vec3dInput defaultSize;

	@Keyword(description = "The default alignment for the instances of this class.",
	         exampleList = {"0.0 0.0 -0.5 m"})
	private final Vec3dInput defaultAlignment;

	private final ArrayList<DisplayModel> displayEntityDefault = new ArrayList<>(1);

	{
		javaClass = new ClassInput( "JavaClass", KEY_INPUTS, null );
		this.addInput( javaClass );

		palette = new StringInput("Palette", KEY_INPUTS, null);
		this.addInput( palette );

		defaultDisplayModel = new EntityInput<>(DisplayModel.class, "DefaultDisplayModel", KEY_INPUTS, null);
		this.addInput(defaultDisplayModel);

		dragAndDrop = new BooleanInput("DragAndDrop", KEY_INPUTS, true);
		this.addInput(dragAndDrop);

		iconFile = new ImageInput("IconFile", KEY_INPUTS, null);
		this.addInput(iconFile);

		defaultSize = new Vec3dInput("DefaultSize", KEY_INPUTS, new Vec3d(1.0d, 1.0d, 1.0d));
		defaultSize.setUnitType(DistanceUnit.class);
		this.addInput(defaultSize);

		defaultAlignment = new Vec3dInput("DefaultAlignment", KEY_INPUTS, new Vec3d(0.0d, 0.0d, 0.0d));
		this.addInput(defaultAlignment);
	}

	public ObjectType() {}

	@Override
	public void updateForInput(Input<?> in) {

		if (in == defaultDisplayModel) {
			displayEntityDefault.clear();
			if (defaultDisplayModel.getValue() != null)
				displayEntityDefault.add(defaultDisplayModel.getValue());
			return;
		}

		if (in == javaClass) {
			getJaamSimModel().addObjectType(this);
		}

		super.updateForInput(in);
	}

	@Override
	public void kill() {
		super.kill();
		getJaamSimModel().removeObjectType(this);
	}

	public Class<? extends Entity> getJavaClass() {
		return javaClass.getValue();
	}

	public String getPaletteName() {
		String s = palette.getValue();
		if (s != null)
			return s;

		return "Default";
	}

	public ArrayList<DisplayModel> getDefaultDisplayModel() {
		return displayEntityDefault;
	}

	public boolean isDragAndDrop() {
		return dragAndDrop.getValue();
	}

	public BufferedImage getIconImage() {
		return iconFile.getValue();
	}

	public Vec3d getDefaultSize() {
		return defaultSize.getValue();
	}

	public Vec3d getDefaultAlignment() {
		return defaultAlignment.getValue();
	}

}
