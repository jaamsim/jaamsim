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

import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vector;
import com.sandwell.JavaSimulation3D.InputAgent;
import com.sandwell.JavaSimulation3D.util.Shape;

import java.lang.String;
import java.lang.NumberFormatException;
import java.util.ArrayList;

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Vector3d;

/**
 * Class used to implement Audition-style string format tests.
 */
public class Tester {

	private static double doubleTolerance = 1.0E-9;
	public static final double AUD_TO_JAVA_UNIT_FACTOR = 0.001;
	private static final String abbrevMonths[] =
	{ "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
	private static final String fullMonths[] =
	{ "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };

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
	 * Implements a string parsing method to test for an integer value using the
	 * Audition semantics for integer formatting.
	 */
	public static boolean isInteger( String testInteger ) {
		try {
			Integer.parseInt( testInteger );
			return true;
		}
		catch( NumberFormatException e ) {
			return false;
		}
	}

	/**
	 * Implements a string parsing method to test for a three-character or all-character month.
	 */
	public static boolean isMonth( String testString ) {
		return (parseMonth(testString) > -1);
	}

	/**
	 * Returns 0 for "Jan" or "January", 1 for "Feb" or "February, ..., 11 for "Dec" or "December"
	 */
	public static int parseMonth( String testString ) {
		String tempString = testString.trim().toUpperCase();
		for( int i = 0; i < 12; i++ ) {
			if (abbrevMonths[i].equals(tempString) ||
				fullMonths[i].equals(tempString)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * If data has only one element or the first two elements are double number -> return true
	 * Possibilities: single number, A Vector list of numbers; an Object
	 */
	public static boolean isOneObject( StringVector data ) {

		if( data.size() == 1 || ( Tester.isDouble( data.get( 0 ) ) && Tester.isDouble( data.get( 1 ) ) ) ) {
			return true;
		}
		return false;
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

	/*
	 * Split the string at spaces with consecutive occurrences counting as a single
	 * delimiter.
	 */
	public static final String[] parseInput(String input) {
		return input.trim().split(" +");
	}

	public static String toTimeString( double timeValue ) {
		String hour = Integer.toString( (int)Math.floor( timeValue ) );
		hour = hour.trim();

		//while( hour.length() < 2 ) {
		//hour = "0" + hour;
		//}

		String minute = Integer.toString( (int)(Math.round( (timeValue - Math.floor( timeValue )) * 60.0 ) % 60.0) );
		minute = minute.trim();

		while( minute.length() < 2 ) {
			minute = "0" + minute;
		}

		return hour + ":" + minute;
	}

	/********************************************************************************
	 * METHODS FOR INPUT SYNTAX CHECK: parse values
	 ********************************************************************************/

	/**
	 * Implements a string parsing method to parse a string into a double value
	 * using the Audition semantics for string conversion.
	 */
	public static double parseDouble( String inputDouble ) {
		// Parse string as time value represented as dd:mm:ss, where dd is in degrees(or hours) and mm is
		// in minutes and ss in seconds
		if( inputDouble.indexOf( ":" ) > -1 ) {
			String[] splitDouble = inputDouble.split( ":" );
			if( splitDouble.length > 3 ) {
				throw new InputErrorException( "The value must contain two colon (:) at most." );
			}

			try {

				// dd:mm
				if( splitDouble.length == 2 ) {
					if( Double.parseDouble( splitDouble[0] ) >= 0 ) {
						return Double.parseDouble( splitDouble[0] )  + Double.parseDouble( splitDouble[1] ) / 60;
					}
					else {
						return Double.parseDouble( splitDouble[0] )  - Double.parseDouble( splitDouble[1] ) / 60;
					}
				}

				// dd:mm:ss
				else {
					if( Double.parseDouble( splitDouble[0] ) >= 0 ) {
						return Double.parseDouble( splitDouble[0] )  + Double.parseDouble( splitDouble[1] ) / 60 + Double.parseDouble( splitDouble[2] ) / (60*60);
					}
					else {
						return Double.parseDouble( splitDouble[0] )  - Double.parseDouble( splitDouble[1] ) / 60 - Double.parseDouble( splitDouble[2] ) / (60*60);
					}
				}
			}
			catch ( NumberFormatException e ) {
				throw new InputErrorException( "" );
			}
		}
		// Parse string as a double
		else {
			try {
				return Double.parseDouble( inputDouble );
			}
			catch ( NumberFormatException e ) {
				throw new InputErrorException( "The value must be a number.  Value = " + inputDouble );
			}
		}
	}

	/**
	 * parse a list of coordinates and if they are in Audition units
	 * convert them to Java units and pop up a warning message
	 * @param data
	 * @return
	 */
	public static Vector parseCoordinatesList( StringVector data ) {
		Input.assertCountEven(data);
		DoubleVector values = Tester.parseDouble( data );
		Vector temp = new Vector( data.size() );
		for( int i = 0; i < data.size(); i += 2 ) {
			temp.addElement( new Vector3d( values.get( i ), values.get( i + 1 ), 0 ) );
		}
		return temp;
	}

	/*
	 * parse a Vector of Colours
	 * does not accept integer colours
	 * @param data
	 * @return
	 */
	public static ArrayList<ColoringAttributes> parseColourVector( StringVector data ) {
		ColoringAttributes col;
		ArrayList<ColoringAttributes> temp = new ArrayList<ColoringAttributes>(data.size());

		ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(data);

		if( splitData.size() > 1 ){
			for( int i = 0; i < splitData.size(); i++ ) {
				col = Tester.parseColour( splitData.get(i) );
				temp.add( col );
			}
		}
		else {
			for( int x = 0; x < data.size(); x++ ) {
				// String colour input
				col = Shape.getColorWithName( data.get( x ) );
				if( col == null ) {
					throw new InputErrorException( "Color " + data.get( x ) + " not found" );
				}
				temp.add( col );
			}
		}
		return temp;
	}

	public static ColoringAttributes parseColour( StringVector data ) {

		// Conserve single colors for backwards compatibility
		if( data.size() == 1 ) {
			if( Tester.isInteger( data.get( 0 ) ) ) {
				InputAgent.logWarning("Integer colours should be replaced with RGB values or colour names");
				int col = Input.parseInteger(data.get(0), 0, 15);
				return Shape.getPresetColor(col);
			}
			else {
				// The colour is named
				ColoringAttributes colAtt = Shape.getColorWithName( data.get( 0 ) );
				if( colAtt == null ) {
					throw new InputErrorException( "Color " + data.get( 0 ) + " not found" );
				}
				else {
					return colAtt;
				}
			}
		}

		// New RGB inputs
		else {
			Input.assertCount(data, 3);
			DoubleVector dbuf = Input.parseDoubleVector(data, 0.0d, Double.POSITIVE_INFINITY);
			return Shape.defineColor((float)dbuf.get(0), (float)dbuf.get(1), (float)dbuf.get(2));
		}
	}

	/**
	 *
	 */
	public static DoubleVector parseDoubleVector( StringVector vec ) {
		DoubleVector doubleVec = new DoubleVector( 1, 1 );

		try {
			for ( int i = 0; i < vec.size(); i++ ) {
				doubleVec.add( Tester.parseDouble( vec.get( i ) ) );
			}
		}
		catch ( NumberFormatException e ) {
			throw new InputErrorException( "The values must be numbers." );
		}

		return doubleVec;
	}

	/**
	 *
	 */
	public static DoubleVector parseDouble( StringVector vec ) {
		DoubleVector doubleVec = new DoubleVector( 1, 1 );

		try {
			for ( int i = 0; i < vec.size(); i++ ) {
				doubleVec.add( Tester.parseDouble( vec.get( i ) ) );
			}
		}
		catch ( NumberFormatException e ) {
			throw new InputErrorException( "The values must be numbers." );
		}

		return doubleVec;
	}

	/********************************************************************************
	 * METHODS FOR INPUT SYNTAX CHECK: check number input data
	 ********************************************************************************/

	/**
	 * Checks whether or not the specified StringVector contains a number of input data elements divisible by k.
	 *
	 * @param data a StringVector containing the input data elements
	 * @param k the number to check if the data count is divisible by
	 * @return true if the number of input data elements is divisible by k
	 * @throws InputErrorException if the number of input data elements is not divisible by k
	 * @author SAW
	 */
	public static boolean checkCountDivisibleBy( StringVector data, int k ) throws InputErrorException {
		if ( ( data.size() % k ) != 0 ) {
			throw new InputErrorException( "The number of input data entries must be divisible by " + k );
		}
		return true;
	}

	/********************************************************************************
	 * METHODS FOR INPUT SYNTAX CHECK: check range of double values
	 ********************************************************************************/

	/**
	 * Checks whether or not the specified number is in the range from the specified
	 * minimum number to the specified maximum number, inclusive.
	 *
	 * @param num the number being checked
	 * @param min the minimum allowed value for the number
	 * @param max the maximum allowed value for the number
	 * @return true if the specified number is in the specified range from min to max,
	 * inclusive
	 * @throws InputErrorException if the specified number is outside of the specified
	 * range
	 * @author SS
	 * *checked*
	 */
	public static boolean checkValueRangeInclusive( double num, double min, double max ) throws InputErrorException {
		if ( num < min || num > max ) {
			throw new InputErrorException( "The value must be in the range from " + min + " to " + max + ", inclusive." );
		}
		return true;
	}

	/**
	 * Checks whether or not the specified number is greater than or equal to the
	 * specified number.
	 *
	 * @param num the number being checked
	 * @param min the minimum allowed value for num
	 * @return true if num is greater than or equal to min
	 * @throws InputErrorException if num is less than min
	 * @author SS
	 * *checked*
	 */
	public static boolean checkValueGreaterOrEqual( double num, double min ) throws InputErrorException {
		try {
			Tester.checkValueRangeInclusive( num, min, Double.POSITIVE_INFINITY );
		}
		catch ( InputErrorException e ) {
			throw new InputErrorException( "The value must be greater than or equal to " + min + "." );
		}
		return true;
	}

	/**
	 * Checks whether or not the specified number is greater than the specified minimum
	 * number.
	 *
	 * @param num the number being checked
	 * @param min the number which the specified num must be greater than
	 * @return true if num is greater than min
	 * @throws InputErrorException if num is not greater than min
	 * @author SS
	 * *checked*
	 */
	public static boolean checkValueGreaterThan( double num, double min ) throws InputErrorException {
		if ( num <= min ) {
			throw new InputErrorException( "The value must be greater than " + min + "." );
		}
		return true;
	}

	/********************************************************************************
	 * METHODS FOR INPUT SYNTAX CHECK: check range of values in a DoubleVector
	 ********************************************************************************/

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

	/********************************************************************************
	 * METHODS FOR INPUT SYNTAX CHECK: check range of values in a IntegerVector
	 ********************************************************************************/

	public static boolean checkValueOneOf( int val, IntegerVector vec ) throws InputErrorException {
		boolean found = false;
		for ( int i = 0; i < vec.size(); i++ ) {
			if ( val == vec.get( i ) ) {
				found = true;
				break;
			}
		}

		if ( !found ) {
			throw new InputErrorException( val + " is not a valid entry." );
		}

		return found;
	}

	/**
	 * Checks whether or not the specified filename, which may be specified with relative
	 * or absolute path, exists.
	 *
	 * @param filePath a file being checked for existence; it may be given with relative
	 * or absolure path
	 * @return true if the specified file exists
	 * @exception InputErrorException if the specified file does not exist
	 * @author SS
	 * *checked*
	 */
	public static boolean checkFileExists( String filePath ) throws InputErrorException {
		if ( !FileEntity.fileExists( filePath ) ) {
			throw new InputErrorException( "The file " + filePath + " does not exist." );
		}
		return true;
	}
}
