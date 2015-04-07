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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Keyword;

public abstract class Unit extends Entity {
	@Keyword(description = "Factor to convert from the specified unit to the System International (SI) unit. " +
					"The factor is entered as A / B, where A is the first entry and B is the second. " +
					"For example, to convert from miles per hour to m/s, the first factor is 1609.344 (meters in one mile) and " +
					"the second factor is 3600 (seconds in one hour).",
			example = "mph  ConversionFactorToSI { 1609.344  3600 }")
	private final SIUnitFactorInput conversionFactorToSI;

	{
		conversionFactorToSI = new SIUnitFactorInput("ConversionFactorToSI", "Key Inputs");
		this.addInput(conversionFactorToSI);
	}

	private static final HashMap<Class<? extends Unit>, Unit>
		preferredUnit = new HashMap<>();

	public Unit() {}

	private static final HashMap<Class<? extends Unit>, String>
		siUnit = new HashMap<>();

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

	public static final void setPreferredUnit(Class<? extends Unit> type, Unit u) {
		preferredUnit.put(type, u);
	}

	public static final <T extends Unit> Unit getPreferredUnit(Class<T> type) {
		return preferredUnit.get(type);
	}

	public static final <T extends Unit> String getDisplayedUnit(Class<T> ut) {
		Unit u = Unit.getPreferredUnit(ut);
		if (u == null)
			return Unit.getSIUnit(ut);
		return u.getName();
	}

	public static final <T extends Unit> double getDisplayedUnitFactor(Class<T> ut) {
		Unit u = Unit.getPreferredUnit(ut);
		if (u == null)
			return 1.0;
		return u.getConversionFactorToSI();
	}

	/**
	 * Return the conversion factor to SI units
	 */
	public double getConversionFactorToSI() {
		return conversionFactorToSI.getSIFactor();
	}

	/**
	 * Return the conversion factor to the given units
	 */
	public double getConversionFactorToUnit( Unit unit ) {
		double f1 = this.getConversionFactorToSI();
		double f2 = unit.getConversionFactorToSI();
		return f1 / f2 ;
	}

	private static class MultPair {
		Class<? extends Unit> a;
		Class<? extends Unit> b;
		public MultPair(Class<? extends Unit> a, Class<? extends Unit> b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public int hashCode() {
			return a.hashCode() ^ b.hashCode();
		}
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof MultPair)) return false;
			MultPair op = (MultPair)other;
			return (a == op.a && b == op.b) ||
			       (a == op.b && b == op.a); // swapped order is still equal
		}
	}

	private static class DivPair {
		Class<? extends Unit> a;
		Class<? extends Unit> b;
		public DivPair(Class<? extends Unit> a, Class<? extends Unit> b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public int hashCode() {
			return a.hashCode() ^ b.hashCode();
		}
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof DivPair)) return false;
			DivPair op = (DivPair)other;
			return (a == op.a && b == op.b);
		}
	}

	private static HashMap<MultPair, Class<? extends Unit>> multRules;
	private static HashMap<DivPair, Class<? extends Unit>> divRules;

	static {
		multRules = new HashMap<>();
		divRules = new HashMap<>();

		// Multiplication rules
		addMultRule(                RateUnit.class,        TimeUnit.class,  DimensionlessUnit.class);
		addMultRule(               SpeedUnit.class,        TimeUnit.class,       DistanceUnit.class);
		addMultRule(        AccelerationUnit.class,        TimeUnit.class,          SpeedUnit.class);
		addMultRule(            MassFlowUnit.class,        TimeUnit.class,           MassUnit.class);
		addMultRule(          VolumeFlowUnit.class,        TimeUnit.class,         VolumeUnit.class);
		addMultRule(        AngularSpeedUnit.class,        TimeUnit.class,          AngleUnit.class);
		addMultRule(               PowerUnit.class,        TimeUnit.class,         EnergyUnit.class);
		addMultRule(            CostRateUnit.class,        TimeUnit.class,           CostUnit.class);
		addMultRule(           ViscosityUnit.class,        TimeUnit.class,  LinearDensityUnit.class);

		addMultRule(            DistanceUnit.class,        RateUnit.class,          SpeedUnit.class);
		addMultRule(               SpeedUnit.class,        RateUnit.class,   AccelerationUnit.class);
		addMultRule(                MassUnit.class,        RateUnit.class,       MassFlowUnit.class);
		addMultRule(              VolumeUnit.class,        RateUnit.class,     VolumeFlowUnit.class);
		addMultRule(               AngleUnit.class,        RateUnit.class,   AngularSpeedUnit.class);
		addMultRule(              EnergyUnit.class,        RateUnit.class,          PowerUnit.class);
		addMultRule(                CostUnit.class,        RateUnit.class,       CostRateUnit.class);
		addMultRule(           ViscosityUnit.class,        RateUnit.class,       PressureUnit.class);

		addMultRule(            DistanceUnit.class,    DistanceUnit.class,           AreaUnit.class);
		addMultRule(       LinearDensityUnit.class,    DistanceUnit.class,           MassUnit.class);
		addMultRule( LinearDensityVolumeUnit.class,    DistanceUnit.class,         VolumeUnit.class);
		addMultRule(                AreaUnit.class,    DistanceUnit.class,         VolumeUnit.class);

		addMultRule(               SpeedUnit.class,       SpeedUnit.class, SpecificEnergyUnit.class);
		addMultRule(       LinearDensityUnit.class,       SpeedUnit.class,       MassFlowUnit.class);
		addMultRule( LinearDensityVolumeUnit.class,       SpeedUnit.class,     VolumeFlowUnit.class);
		addMultRule(                AreaUnit.class,       SpeedUnit.class,     VolumeFlowUnit.class);

		addMultRule(       EnergyDensityUnit.class,      VolumeUnit.class,         EnergyUnit.class);
		addMultRule(             DensityUnit.class,      VolumeUnit.class,           MassUnit.class);
		addMultRule(            PressureUnit.class,      VolumeUnit.class,         EnergyUnit.class);

		addMultRule(       EnergyDensityUnit.class,  VolumeFlowUnit.class,          PowerUnit.class);
		addMultRule(             DensityUnit.class,  VolumeFlowUnit.class,       MassFlowUnit.class);
		addMultRule(            PressureUnit.class,  VolumeFlowUnit.class,          PowerUnit.class);
	}

	public static void addMultRule(Class<? extends Unit> a, Class<? extends Unit> b, Class<? extends Unit> product) {
		MultPair key = new MultPair(a, b);
		multRules.put(key, product);

		// Add the corresponding division rules
		addDivRule(product, a, b);
		addDivRule(product, b, a);
	}

	public static void addDivRule(Class<? extends Unit> num, Class<? extends Unit> denom, Class<? extends Unit> product) {
		DivPair key = new DivPair(num, denom);
		divRules.put(key, product);
	}

	// Get the new unit type resulting from multiplying two unit types
	public static Class<? extends Unit> getMultUnitType(Class<? extends Unit> a, Class<? extends Unit> b) {
		if (a == DimensionlessUnit.class)
			return b;
		if (b == DimensionlessUnit.class)
			return a;

		return multRules.get(new MultPair(a, b));
	}

	// Get the new unit type resulting from dividing two unit types
	public static Class<? extends Unit> getDivUnitType(Class<? extends Unit> num, Class<? extends Unit> denom) {

		if (denom == DimensionlessUnit.class)
			return num;

		if (num == denom)
			return DimensionlessUnit.class;

		return divRules.get(new DivPair(num, denom));
	}
}
