/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.units;

import java.util.HashMap;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;

public abstract class Unit extends Entity {
	@Keyword(description = "Factor to convert from the specified unit to the System International (SI) unit. " +
					"The factor is entered as A / B, where A is the first entry and B is the second. " +
					"For example, to convert from miles per hour to m/s, the first factor is 1609.344 (meters in one mile) and " +
					"the second factor is 3600 (seconds in one hour).",
			example = "mph  ConversionFactorToSI { 1609.344  3600 }")
	private final ValueListInput conversionFactorToSI;

	@Keyword(description = "The preferred unit for formatting output values in the OutputViewer for " +
	                       "this type of Unit.",
	         example = "mph PreferredUnit { km/h }")
	private final EntityInput<? extends Unit> prefInput;

	private static final DoubleVector defFactors;

	static {
		defFactors = new DoubleVector(1);
		defFactors.add(1.0d);
	}

	{
		conversionFactorToSI = new ValueListInput("ConversionFactorToSI", "Key Inputs", defFactors);
		conversionFactorToSI.setUnitType(DimensionlessUnit.class);
		conversionFactorToSI.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		conversionFactorToSI.setValidCountRange( 1, 2 );
		this.addInput( conversionFactorToSI );

		prefInput = getPrefInput(this.getClass());
		this.addInput(prefInput);
	}

	public Unit() {}

	@Override
	public void kill() {

		if (prefInput.getValue() == this)
			prefInput.reset();

		super.kill();
	}


	private static final HashMap<Class<? extends Unit>, String>
		siUnit = new HashMap<Class<? extends Unit>, String>();

	public static final void setSIUnit(Class<? extends Unit> unitType, String si) {
		siUnit.put(unitType, si);
	}

	/**
	 * Get the SI unit for the given unit type.
	 * @param unitType
	 * @return a string describing the SI unit, or if one has not been defined: 'SI'
	 */
	public static final String getSIUnit(Class<? extends Unit> unitType) {
		String unit = siUnit.get(unitType);
		if (unit != null)
			return unit;

		return "SI";
	}

	private static final HashMap<Class<? extends Unit>, EntityInput<? extends Unit>>
		prefUnit = new HashMap<Class<? extends Unit>, EntityInput<? extends Unit>>();

	private static final <T extends Unit> EntityInput<? extends Unit> getPrefInput(Class<T> type) {
		EntityInput<? extends Unit> inp = prefUnit.get(type);
		if (inp == null) {
			inp = new EntityInput<T>(type, "PreferredUnit", "Key Inputs", null);
			prefUnit.put(type, inp);
		}
		return inp;
	}

	public static final <T extends Unit> Unit getPreferredUnit(Class<T> type) {
		EntityInput<? extends Unit> inp = prefUnit.get(type);
		if (inp == null)
			return null;
		else
			return inp.getValue();
	}

	/**
	 * Return the conversion factor to SI units
	 */
	public double getConversionFactorToSI() {
		DoubleVector d = conversionFactorToSI.getValue();

		// if the conversionFactorToSI input has one value, we assume a divisor
		// of 1.0
		if (d.size() == 1)
			return d.get(0);

		return d.get(0) / d.get(1);
	}

	/**
	 * Return the conversion factor to the given units
	 */
	public double getConversionFactorToUnit( Unit unit ) {
		double f1 = this.getConversionFactorToSI();
		double f2 = unit.getConversionFactorToSI();
		return f1 / f2 ;
	}
}
