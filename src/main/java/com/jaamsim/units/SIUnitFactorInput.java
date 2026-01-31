/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2026 JaamSim Software Inc.
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
package com.jaamsim.units;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;

public class SIUnitFactorInput extends Input<double[]> {
	private static final double[] defFactors = { 1.0d };
	private double siFactor;

	public SIUnitFactorInput(String key, String cat) {
	    super(key, cat, defFactors);
    }

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, 1e-15d, Double.POSITIVE_INFINITY, DimensionlessUnit.class);
		Input.assertCountRange(temp, 1, 2);
		double[] tmp = new double[temp.size()];
		tmp[0] = temp.get(0);
		if (temp.size() == 2)
			tmp[1] = temp.get(1);

		calculateSI(tmp);
		value = tmp;
	}

	final double getSIFactor() {
		return siFactor;
	}

	private void calculateSI(double[] factors) {
		siFactor = factors[0];
		if (factors.length == 2)
			siFactor /= factors[1];
	}

	@Override
	public void setDefaultValue(double[] val) {
		super.setDefaultValue(val);
		calculateSI(value);
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		return "1.0";
	}

	@Override
	public Class<?> getReturnType() {
		return double[].class;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return DimensionlessUnit.class;
	}
}
