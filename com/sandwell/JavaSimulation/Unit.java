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
package com.sandwell.JavaSimulation;

public abstract class Unit extends Entity {
	// if one entry, it is the number of SI units per this unit.  i.e. multiply by this number to convert this unit into SI units
	// if two entries, it is the number of SI units per this unit expressed as a fraction.  i.e. multiply by the first number and divide by the second number to convert this unit into SI units
	private final DoubleListInput conversionFactorToSI;
	private final AliasListInput alias; // a list of strings that can be used as alternate names of the object

	{
		conversionFactorToSI = new DoubleListInput( "ConversionFactorToSI", "Key Inputs", null );
		conversionFactorToSI.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		conversionFactorToSI.setUnits( "(SI units)/unit" );
		conversionFactorToSI.setValidCountRange( 1, 2 );
		this.addInput( conversionFactorToSI, true );

		alias = new AliasListInput( "Alias", "Key Inputs", null, this );
		this.addInput( alias, true );
	}

	public Unit() {
	}

	/**
	 * Return the conversion factor to SI units
	 */
	public double getConversionFactorToSI() {

		if( conversionFactorToSI.getValue() == null ) {
			throw new InputErrorException( this.getName() + " missing conversion factor to SI" );
		}

		// if the conversionFactorToSI input has one value, then it is the conversion factor to SI units
		if( conversionFactorToSI.getValue().size() == 1 ) {
			return conversionFactorToSI.getValue().get( 0 );
		}
		else {
			// if the conversionFactorToSI input has two values,
			// then the first value divided by the second value is the conversion factor to SI units
			if( conversionFactorToSI.getValue().size() == 2 ) {
				return conversionFactorToSI.getValue().get( 0 ) / conversionFactorToSI.getValue().get( 1 );
			}
			else {
				throw new InputErrorException( this.getName() + " ConversionFactorToSI must have 1 or 2 values" );
			}
		}
	}

	/**
	 * Return the conversion factor to the given units
	 */
	public double getConversionFactorToUnit( Unit unit ) {
		Double factor1 = this.getConversionFactorToSI();
		Double factor2 = unit.getConversionFactorToSI();
		if( unit.getClass() == this.getClass() && factor1 != null && factor2 != null ) {
			return factor1 / factor2 ;
		}
		else {
			throw new InputErrorException( "Cannot convert from " + this + " to " + unit );
		}
	}

	public void kill() {
		if( alias.getValue() != null ) {
			for( String str : alias.getValue() ) {
				this.removeAlias( str );
			}
		}
		super.kill();
	}

}
