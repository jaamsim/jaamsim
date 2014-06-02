package com.sandwell.JavaSimulation;

import com.jaamsim.input.Input;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class Vec3dInput extends Input<Vec3d> {
	private Class<? extends Unit> unitType = DimensionlessUnit.class;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public Vec3dInput(String key, String cat, Vec3d def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> units) {
		unitType = units;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(kw, minValue, maxValue, unitType);
		Input.assertCountRange(temp, 1, 3);

		// pad the vector to have 3 elements
		while (temp.size() < 3) {
			temp.add(0.0d);
		}

		value = new Vec3d(temp.get(0), temp.get(1), temp.get(2));
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder();
		tmp.append(defValue.x);
		tmp.append(SEPARATOR);
		tmp.append(defValue.y);
		tmp.append(SEPARATOR);
		tmp.append(defValue.z);
		if (unitType != Unit.class) {
			tmp.append(SEPARATOR);
			tmp.append(Unit.getSIUnit(unitType));
		}

		return tmp.toString();
	}
}
