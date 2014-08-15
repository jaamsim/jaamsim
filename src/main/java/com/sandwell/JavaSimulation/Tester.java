/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002 Ausenco Engineering Canada Inc.
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

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.InputErrorException;


/**
 * Class used to implement Audition-style string format tests.
 */
public class Tester {

	private static double doubleTolerance = 1.0E-9;

	/**
	 * Set the tolerance level for double valued comparisons.  The default value
	 * for double comparisons is 1.0E-10.
	 */
	public static void setDoubleTolerance( double newTolerance ) {
		if( !(newTolerance < 0.0) ) {
			doubleTolerance = newTolerance;
		}
	}

	/**
	 * Implements a string parsing method to test for a double value using the
	 * Audition semantics for double formatting.
	 */
	public static boolean isDouble( String testDouble ) {
		String testString = testDouble.trim().replaceAll( "[0-9]", "" );

		if( testString.startsWith( "-" ) ) {
			testString = testString.replaceFirst( "-", "" );
		}

		if( testString.equals( ":" ) ) {
			return true;
		}

		try {
			Double.parseDouble( testDouble );
			return true;
		}
		catch( NumberFormatException e ) {
			return false;
		}
	}

	/**
	 * Implements a string parsing method to test for a date value using the
	 * Audition semantics for date formatting.
	 */
	public static boolean isDate( String testString ) {
		String tempString = testString.trim();

		if( !(tempString.length() == 8 || tempString.length() == 10) ) {
			return false;
		}

		int dateLength = tempString.length();
		String day = tempString.substring( dateLength - 3, dateLength );
		String month = tempString.substring( dateLength - 6, dateLength - 3 );
		String year = tempString.substring( 0, dateLength - 6 );

		//check day
		if( !(day.matches( "-[0-2][0-9]" ) || day.matches( "-3[0-1]" )) ) {
			return false;
		}

		// check month
		if( !(month.matches( "-0[0-9]" ) || month.matches( "-1[0-2]" )) ) {
			return false;
		}

		// check year
		if( !(year.matches( "[0-9][0-9]" ) || year.matches( "[0-9][0-9][0-9][0-9]" )) ) {
			return false;
		}

		// Passed all tests return true
		return true;
	}

	/**
	 * Compare two double values and return true if they are within doubleTolerance
	 * of each other.
	 */
	public static boolean equalCheckTolerance( double first, double second ) {
		return (Math.abs( first - second ) < doubleTolerance);
	}

	/**
	 * Compare two double values using the >= operator, applying the doubleTolerance
	 * value.
	 */
	public static boolean greaterOrEqualCheckTolerance( double first, double second ) {
		return (first + doubleTolerance) > second;
	}

	/**
	 * Compare two double values using the > operator, applying the doubleTolerance
	 * value.
	 */
	public static boolean greaterCheckTolerance( double first, double second ) {
		return (first - doubleTolerance) >= second;
	}

	/**
	 * Compare two double values using the <= operator, applying the doubleTolerance
	 * value.
	 */
	public static boolean lessOrEqualCheckTolerance( double first, double second ) {
		return (first - doubleTolerance) < second;
	}

	/**
	 * Compare two double values using the < operator, applying the doubleTolerance
	 * value.
	 */
	public static boolean lessCheckTolerance( double first, double second ) {
		return (first + doubleTolerance) <= second;
	}

	/**
	 * Compare two double values converting to the corresponding system long
	 * value and applying the == operator.
	 */
	public static boolean equalCheckTimeStep( double first, double second ) {
		return (calculateEventTime(first) == calculateEventTime(second));
	}

	/**
	 * Compare two double values converting to the corresponding system long
	 * value and applying the > operator.
	 */
	public static boolean greaterCheckTimeStep( double first, double second ) {
		return (calculateEventTime(first) > calculateEventTime(second));
	}

	/**
	 * Compare two double values converting to the corresponding system long
	 * value and applying the < operator.
	 */
	public static boolean lessCheckTimeStep( double first, double second ) {
		return (calculateEventTime(first) < calculateEventTime(second));
	}

	/**
	 * Compare two double values converting to the corresponding system long
	 * value and applying the >= operator.
	 */
	public static boolean greaterOrEqualCheckTimeStep( double first, double second ) {
		return (calculateEventTime(first) >= calculateEventTime(second));
	}

	/**
	 * Compare two double values converting to the corresponding system long
	 * value and applying the <= operator.
	 */
	public static boolean lessOrEqualCheckTimeStep( double first, double second ) {
		return (calculateEventTime(first) <= calculateEventTime(second));
	}

	private static long calculateEventTime(double time) {
		return Math.round(time * Simulation.getSimTimeFactor());
	}

	public static void checkValueOrderAscending( DoubleVector vec ) throws InputErrorException {
		for ( int i = 0; i < vec.size() - 1; i++ ) {
			if ( vec.get( i ) >= vec.get( i + 1 ) ) {
				throw new InputErrorException( "The values must be in the ascending order." );
			}
		}
	}

	public static void checkValueOrderDescending( DoubleVector vec ) throws InputErrorException {
		for ( int i = 0; i < vec.size() - 1; i++ ) {
			if ( vec.get( i + 1 ) >= vec.get( i ) ) {
				throw new InputErrorException( "The values must be in the descending order." );
			}
		}
	}

	public static void checkValueOrderAscendingFromTo ( DoubleVector vec, int from, int to ) throws InputErrorException {
		boolean error = false;

		if ( vec.get( 0 ) != from || vec.get( vec.size() - 1 ) != to ) {
			error = true;
		}

		if ( !error ) {
			try {
				Tester.checkValueOrderAscending( vec );
			}
			catch ( InputErrorException e ) {
				error = true;
			}
		}

		if ( error ) {
			throw new InputErrorException( "The values must be in the ascending order from " + from + " to " + to + "." );
		}
	}

	public static double min( double... values ) {

		double min = Double.POSITIVE_INFINITY;

		for( double each : values ) {
			min = Math.min(min, each);
		}
		return min;
	}
}
