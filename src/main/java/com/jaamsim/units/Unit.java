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

import com.sandwell.JavaSimulation.AliasListInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Keyword;

public abstract class Unit extends Entity {
	private static final HashMap<Class<? extends Unit>, String> siUnit;

	@Keyword(description = "Factor to convert from the specified unit to the System International (SI) unit. " +
					"The factor is entered as A / B, where A is the first entry and B is the second. " +
					"For example, to convert from miles per hour to m/s, the first factor is 1609.344 (meters in one mile) and " +
					"the second factor is 3600 (seconds in one hour).",
			example = "mph  ConversionFactorToSI { 1609.344  3600 }")
	private final DoubleListInput conversionFactorToSI;

	@Keyword(description = "Alternative names for the same unit.  For example, the unit 'Year' could have aliases of " +
					"'y' or 'yr'. With these aliases, the following inputs are equivalent: { 1.0 year }, { 1.0 y }, and { 1.0 yr }.",
			example = "Year Alias { y  yr }")
	private final AliasListInput alias;

	private static final DoubleVector defFactors;

	static {
		siUnit = new HashMap<Class<? extends Unit>, String>();

		defFactors = new DoubleVector(1);
		defFactors.add(1.0d);
	}

	{
		conversionFactorToSI = new DoubleListInput("ConversionFactorToSI", "Key Inputs", defFactors);
		conversionFactorToSI.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		conversionFactorToSI.setValidCountRange( 1, 2 );
		this.addInput( conversionFactorToSI, true );

		alias = new AliasListInput( "Alias", "Key Inputs", null, this );
		this.addInput( alias, true );
	}

	public Unit() {}

	@Override
	public void kill() {
		if( alias.getValue() != null ) {
			for( String str : alias.getValue() ) {
				this.removeAlias( str );
			}
		}
		super.kill();
	}

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
