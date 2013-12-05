/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

import java.util.HashMap;

public class ValueTable<T extends Entity> extends HashMap<T, TimeValue> {

	private TimeValue defaultValue;

	public ValueTable( double def ) {
		super();
		defaultValue = new TimeValue( def );
	}

	public void setDefault( TimeValue val ) {
		defaultValue = val;
	}

	public TimeValue getDefault() {
		return defaultValue;
	}

	public double getNextValueForEntity_Time( T ent, double t ) {

		TimeValue val = this.get( ent );
		if( val == null)
			return defaultValue.getNextValueForTime( t );

		return val.getNextValueForTime( t );
	}

	public double getCurrentValueForEntity( T ent ) {

		TimeValue val = this.get( ent );
		if( val == null)
			return defaultValue.getCurrentValue();

		return val.getCurrentValue();
	}

	public double getExpectedValueForEntity_AtTime( T ent, double t ) {

		TimeValue val = this.get( ent );
		if( val == null)
			return defaultValue.getExpectedValueForTime(t);

		return val.getExpectedValueForTime(t);
	}

	public double getFirstNonZeroExpectedValueForTime(double t) {

		// find the first non zero entry
		for( TimeValue each : this.values() ) {
			if( each.getExpectedValueForTime(t) > 0 ) {
				return each.getExpectedValueForTime(t);
			}
		}

		return defaultValue.getExpectedValueForTime(t);
	}

	public boolean hasNonZeroExpectedValueForTime(double t) {

		// find the first non zero entry
		for( TimeValue each : this.values() ) {
			if( each.getExpectedValueForTime(t) > 0 ) {
				return true;
			}
		}

		if( defaultValue.getExpectedValueForTime(t) > 0 )
			return true;

		return false;
	}

	public double getMinValueForEntity_AtTime( T ent, double t ) {

		TimeValue val = this.get( ent );
		if( val == null)
			return defaultValue.getMinValueFor(t);

		return val.getMinValueFor(t);
	}

	public double getMaxValueForEntity_AtTime( T ent, double t ) {

		TimeValue val = this.get( ent );
		if( val == null)
			return defaultValue.getMaxValueFor(t);

		return val.getMaxValueFor(t);
	}

	public void initialize() {
		for( TimeValue each : this.values() ) {
			each.initialize();
		}
		defaultValue.initialize();
	}
}
