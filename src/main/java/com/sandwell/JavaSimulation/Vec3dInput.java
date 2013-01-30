package com.sandwell.JavaSimulation;

import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.Unit;

public class Vec3dInput extends Input<Vec3d> {

	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;

	public Vec3dInput(String key, String cat, Vec3d def) {
		super(key, cat, def);
	}

	public void parse(StringVector input)
	throws InputErrorException {
		DoubleVector temp;

		// If there is more than one value, and the last one is not a number, then assume it is a unit
		if( input.size() > 1 && !Tester.isDouble( input.get( input.size()-1 ) ) ) {

			// Determine the units
			Unit unit = Input.parseUnits(input.get(input.size()- 1));

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity( unitString.replaceAll("[()]", "").trim(), Unit.class );
			if( defaultUnit == null ) {
				throw new InputErrorException( "Could not determine default units " + unitString );
			}

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

			// Parse and convert the values
			Input.assertCountRange(input.subString(0,input.size()-2), 1, 3);
			temp = Input.parseDoubleVector(input.subString(0,input.size()-2), minValue, maxValue, conversionFactor);
		}
		else {
			// Parse the values
			Input.assertCountRange(input, 1, 3);
			temp = Input.parseDoubleVector(input, minValue, maxValue);

			if( unitString.length() > 0 )
				InputAgent.logWarning( "Missing units.  Assuming %s.", unitString );
		}

		// pad the vector to have 3 elements
		while (temp.size() < 3) {
			temp.add(0.0d);
		}

		value = new Vec3d(temp.get(0), temp.get(1), temp.get(2));
		this.updateEditingFlags();
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

		if (!unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}

		return tmp.toString();
	}
}
