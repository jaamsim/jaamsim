/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ListInput;

/**
 * AttributeDefinitionListInput is an object for parsing inputs consisting of a list of
 * Attribute definitions using the syntax:
 * Entity AttributeDefinitionList { { AttibuteName1 Value1 Unit1 } { AttibuteName2 Value2 Unit2 } ... }
 * @author Harry King
 */
public class AttributeDefinitionListInput extends ListInput<ArrayList<AttributeHandle>> {

	private Entity ent;

	public AttributeDefinitionListInput(Entity e, String key, String cat, ArrayList<AttributeHandle> def) {
		super(key, cat, def);
		ent = e;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<AttributeHandle> temp = new ArrayList<AttributeHandle>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2, 3);
			try {
				// Parse the attribute name
				String name = subArg.getArg(0);
				if (OutputHandle.hasOutput(ent.getClass(), name)) {
					throw new InputErrorException("Attribute name is the same as existing output name: %s", name);
				}

				// Parse the unit type
				double factor = 1.0;
				Class<? extends Unit> unitType = DimensionlessUnit.class;
				if (subArg.numArgs() == 3) {
					Unit unit = Input.parseUnits(subArg.getArg(2));
					unitType = unit.getClass();
					factor = unit.getConversionFactorToSI();
				}

				// Parse the initial value
				double val;
				try {
					val = factor * Double.valueOf(subArg.getArg(1));
				} catch (Exception e) {
					throw new InputErrorException(INP_ERR_DOUBLE, subArg.getArg(1));
				}

				// Save the data for this attribute
				AttributeHandle h = new AttributeHandle(ent, name);
				h.setUnitType(unitType);
				h.setInitialValue(val);
				h.setValue(val);
				temp.add(h);

			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}

		// Save the data for each attribute
		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getValueString() {
		if( value == null) return "";
		return this.getInputString(value);
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty()) return NO_VALUE;
		return this.getInputString(defValue);
	}

	private String getInputString(ArrayList<AttributeHandle> handleList) {

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < handleList.size(); i++) {
			if (i > 0) tmp.append(SEPARATOR);
			AttributeHandle h = handleList.get(i);
			tmp.append("{ ");
			tmp.append(h.getName());
			tmp.append(SEPARATOR);

			double val = h.getInitialValue();
			String unitString = Unit.getSIUnit(h.getUnitType());

			// Check for a preferred unit
			Unit unit = Unit.getPreferredUnit(h.getUnitType());
			if (unit != null) {
				unitString = unit.toString();
				val = h.getValueAsDouble(0.0d, 0.0d, unit);
			}
			tmp.append(val);

			// Print the unit unless it is dimensionless
			if (h.getUnitType() != DimensionlessUnit.class) {
				tmp.append(SEPARATOR);
				tmp.append(unitString);
			}
			tmp.append(" }");
		}
		return tmp.toString();
	}

}
