/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public interface TextEntity {
	public String getFontName();
	public double getTextHeight();
	public String getTextHeightString();
	public int getStyle();
	public boolean isBold();
	public boolean isItalic();
	public Color4d getFontColor();
	public boolean getDropShadow();
	public Color4d getDropShadowColor();
	public Vec3d getDropShadowOffset();

	public static ArrayList<String> getFontsInUse() {
		ArrayList<String> ret = new ArrayList<>();
		for (DisplayEntity ent : Entity.getClonesOfIterator(DisplayEntity.class, TextEntity.class)) {
			TextEntity textEnt = (TextEntity) ent;
			if (ret.contains(textEnt.getFontName()))
				continue;
			ret.add(textEnt.getFontName());
		}
		Collections.sort(ret);
		return ret;
	}

	public static ArrayList<Color4d> getFontColoursInUse() {
		ArrayList<Color4d> ret = new ArrayList<>();
		for (DisplayEntity ent : Entity.getClonesOfIterator(DisplayEntity.class, TextEntity.class)) {
			TextEntity textEnt = (TextEntity) ent;
			if (ret.contains(textEnt.getFontColor()))
				continue;
			ret.add(textEnt.getFontColor());
		}
		Collections.sort(ret, ColourInput.colourComparator);
		return ret;
	}

}
