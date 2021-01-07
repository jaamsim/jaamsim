/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2021 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.math.Color4d;

public class ColorListInput extends ListInput<ArrayList<Color4d>>  {

	public ColorListInput(String key, String cat, ArrayList<Color4d> def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		value = Input.parseColorVector(thisEnt.getJaamSimModel(), kw);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_COLOR_LIST;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {

			// blank space between elements
			if (tmp.length() > 0)
				tmp.append(SEPARATOR);

			Color4d col = defValue.get(i);
			String colorName = ColourInput.getColorName(col);
			if (colorName == null)
				tmp.append( String.format("{%s%.0f%s%.0f%s%.0f%s}", SEPARATOR, col.r * 255,
				   SEPARATOR, col.g * 255, SEPARATOR, col.b * 255, SEPARATOR ));
			else
				tmp.append( String.format("{%s%s%s}", SEPARATOR, colorName, SEPARATOR));
		}

		return tmp.toString();
	}
}
