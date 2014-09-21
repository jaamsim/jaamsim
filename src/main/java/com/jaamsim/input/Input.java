/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2012 Ausenco Engineering Canada Inc.
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.BooleanVector;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Group;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation3D.Clock;

public abstract class Input<T> {
	protected static final String INP_ERR_COUNT = "Expected an input with %s value(s), received: %s";
	protected static final String INP_ERR_RANGECOUNT = "Expected an input with %d to %d values, received: %s";
	protected static final String INP_ERR_EVENCOUNT = "Expected an input with even number of values, received: %s";
	protected static final String INP_ERR_ODDCOUNT = "Expected an input with odd number of values, received: %s";
	protected static final String INP_ERR_BOOLEAN = "Expected a boolean value, received: %s";
	protected static final String INP_ERR_INTEGER = "Expected an integer value, received: %s";
	protected static final String INP_ERR_INTEGERRANGE = "Expected an integer between %d and %d, received: %d";
	protected static final String INP_ERR_DOUBLE = "Expected an numeric value, received: %s";
	protected static final String INP_ERR_DOUBLERANGE = "Expected a number between %f and %f, received: %f";
	protected static final String INP_ERR_TIME = "Expected a time value (hh:mm or hh:mm:ss), received: %s";
	protected static final String INP_ERR_TIMEVALUE = "Expected a numeric value, 12 numeric values, or a probabilty distribution, received: %s";
	protected static final String INP_ERR_BADSUM = "List must sum to %f, received:%f";
	protected static final String INP_ERR_BADCHOICE = "Expected one of %s, received: %s";
	protected static final String INP_ERR_ELEMENT = "Error parsing element %d: %s";
	protected static final String INP_ERR_ENTNAME = "Could not find an Entity named: %s";
	protected static final String INP_ERR_NOUNITFOUND = "A unit is required, could not parse '%s' as a %s";
	protected static final String INP_ERR_NOTUNIQUE = "List must contain unique entries, repeated entry: %s";
	protected static final String INP_ERR_NOTVALIDENTRY = "List must not contain: %s";
	protected static final String INP_ERR_ENTCLASS = "Expected a %s, %s is a %s";
	protected static final String INP_ERR_INTERFACE = "Expected an object implementing %s, %s does not";
	protected static final String INP_ERR_UNITS = "Unit types do not match";
	protected static final String INP_ERR_UNITUNSPECIFIED = "Unit type has not been specified";
	protected static final String INP_ERR_NOTSUBCLASS = "Expected a subclass of %s, got %s";
	protected static final String INP_ERR_BADDATE = "Expected a valid RFC8601 datetime, got: %s";
	protected static final String INP_VAL_LISTSET = "Values found for %s without %s being set";
	protected static final String INP_VAL_LISTSIZE = "%s and %s must be of equal size";

	protected static final String NO_VALUE = "{ }";
	protected static final String POSITIVE_INFINITY = "Infinity";
	protected static final String NEGATIVE_INFINITY = "-Infinity";
	protected static final String SEPARATOR = "  ";

	private final String keyword; // the preferred name for the input keyword
	private final String category;

	protected T defValue;
	protected T value;

	private boolean edited; // indicates if input has been edited for this entity
	private boolean hidden; // Hide this input from the EditBox
	protected String valueString; // value from .cfg file

	public static class ParseContext {
		public URI context;
		public String jail;
	}

	public Input(String key, String cat, T def) {
		keyword = key;
		category = cat;
		setDefaultValue(def);

		edited = false;
		hidden = false;
		valueString = "";
	}

	public void reset() {
		this.setDefaultValue( this.getDefaultValue() );
		valueString = "";
		edited = false;
	}

	@Override
	public String toString() {
		return String.format("%s", value);
	}

	public final String getKeyword() {
		return keyword;
	}

	public final String getCategory() {
		return category;
	}

	public void setDefaultValue(T val) {
		defValue = val;
		value = val;
	}

	public T getDefaultValue() {
		return defValue;
	}

	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder(defValue.toString());

		if (tmp.length() == 0)
			return NO_VALUE;

		return tmp.toString();
	}

	/**
	 * Returns the input String used to designate no value or null.
	 *
	 * @return - the NO_VALUE input String.
	 */
	public static String getNoValue() {
		return NO_VALUE;
	}

	public T getValue() {
		return value;
	}

	public void setHidden(boolean hide) {
		hidden = hide;
	}

	public boolean getHidden() {
		return hidden;
	}

	public void setEdited(boolean bool) {
		edited = bool;
	}

	public boolean isEdited() {
		return edited;
	}

	public String getValueString() {
		return valueString;
	}

	public void setValueString(String str) {
		valueString = str;
	}

	public abstract void parse(KeywordIndex kw) throws InputErrorException;


	public static void assertCount(DoubleVector input, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts == null || counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == input.size())
				return;
		}

		// Input size is not equal to any of the specified counts
		throw new InputErrorException(INP_ERR_COUNT, Arrays.toString(counts), input.toString());
	}

	public static void assertCount(KeywordIndex kw, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == kw.numArgs())
				return;
		}

		// Input size is not equal to any of the specified counts
		throw new InputErrorException(INP_ERR_COUNT, Arrays.toString(counts), kw.argString());
	}

	public static void assertCountRange(KeywordIndex kw, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(kw, min);
			return;
		}

		if (kw.numArgs() < min || kw.numArgs() > max)
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, kw.argString());
	}

	public static void assertCount(List<String> input, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == input.size())
				return;
		}

		// Input size is not equal to any of the specified counts
		throw new InputErrorException(INP_ERR_COUNT, Arrays.toString(counts), input.toString());
	}

	public static void assertCountRange(DoubleVector input, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(input, min);
			return;
		}

		if (input.size() < min || input.size() > max)
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, input.toString());
	}

	public static void assertCountRange(List<String> input, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(input, min);
			return;
		}

		if (input.size() < min || input.size() > max)
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, input.toString());
	}

	public static void assertCountEven(KeywordIndex kw)
	throws InputErrorException {
		if ((kw.numArgs() % 2) != 0)
			throw new InputErrorException(INP_ERR_EVENCOUNT, kw.argString());
	}

	public static void assertCountOdd(KeywordIndex kw)
	throws InputErrorException {
		if ((kw.numArgs() % 2) == 0)
			throw new InputErrorException(INP_ERR_ODDCOUNT, kw.argString());
	}

	public static <T extends Entity> void assertNotPresent(ArrayList<? super T> list, T ent)
	throws InputErrorException {
		if (list.contains(ent))
			throw new InputErrorException(INP_ERR_NOTVALIDENTRY, ent.getName());
	}

	public static void assertSumTolerance(DoubleVector vec, double sum, double tol)
	throws InputErrorException {
		// Vector sum is within tolerance of given sum, no error
		if (Math.abs(vec.sum() - sum) < tol)
			return;

		throw new InputErrorException(INP_ERR_BADSUM, sum, vec.sum());
	}

	public static <T> T parse(List<String> data, Class<T> aClass, String units, double minValue, double maxValue, int minCount, int maxCount, Class<? extends Unit> unitType) {

		if( aClass == Double.class ) {
			if( units != null )
				return aClass.cast( Input.parseDouble( data, minValue, maxValue, units) );
			else{
				DoubleVector tmp = Input.parseDoubles( data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType );
				Input.assertCount(tmp, 1);
				return aClass.cast( tmp.get(0));
			}
		}

		if( aClass == DoubleVector.class ) {
			if( units != null ){
				DoubleVector value = Input.parseDoubleVector( data, minValue, maxValue, units);
				if (value.size() < minCount || value.size() > maxCount)
					throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, data);
				return aClass.cast( value );
			}
			else {
				DoubleVector tmp = Input.parseDoubles( data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType );
				return aClass.cast( tmp );
			}
		}

		if( Entity.class.isAssignableFrom(aClass) ) {
			Class<? extends Entity> temp = aClass.asSubclass(Entity.class);
			Input.assertCount(data, 1, 1);
			return aClass.cast( Input.parseEntity(data.get(0), temp) );
		}

		if( aClass == Boolean.class ) {
			Input.assertCount(data, 1);
			Boolean value = Boolean.valueOf(Input.parseBoolean(data.get(0)));
			return aClass.cast(value);
		}

		if( aClass == Integer.class ) {
			Input.assertCount(data, 1);
			Integer value = Input.parseInteger(data.get( 0 ), (int)minValue, (int)maxValue);
			return aClass.cast(value);
		}

		if( aClass == SampleProvider.class ) {

			// Try to parse as a constant value
			try {
				DoubleVector tmp = Input.parseDoubles(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
				Input.assertCount(tmp, 1);
				return aClass.cast( new SampleConstant(unitType, tmp.get(0)) );
			}
			catch (InputErrorException e) {}

			// If not a constant, try parsing a SampleProvider
			Input.assertCount(data, 1);
			Entity ent = Input.parseEntity(data.get(0), Entity.class);
			SampleProvider s = Input.castImplements(ent, SampleProvider.class);
			if( s.getUnitType() != UserSpecifiedUnit.class )
				Input.assertUnitsMatch(unitType, s.getUnitType());
			return aClass.cast(s);
		}

		// TODO - parse other classes
//		if( aClass == IntegerVector.class ) {
//		}

		throw new InputErrorException("%s is not supported for parsing yet", aClass);
	}

	public static boolean parseBoolean(String data)
	throws InputErrorException {
		if ("TRUE".equals(data)) {
			return true;
		}

		if ("FALSE".equals(data)) {
			return false;
		}

		throw new InputErrorException(INP_ERR_BOOLEAN, data);
	}

	public static BooleanVector parseBooleanVector(KeywordIndex kw)
	throws InputErrorException {
		BooleanVector temp = new BooleanVector(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			try {
				boolean element = Input.parseBoolean(kw.getArg(i));
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static BooleanVector parseBooleanVector(List<String> input)
	throws InputErrorException {
		BooleanVector temp = new BooleanVector(input.size());

		for (int i = 0; i < input.size(); i++) {
			try {
				boolean element = Input.parseBoolean(input.get(i));
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static ArrayList<Color4d> parseColorVector(KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<Color4d> temp = new ArrayList<Color4d>(subArgs.size());

		for (int i = 0; i < subArgs.size(); i++) {
			try {
				Color4d element = Input.parseColour(subArgs.get(i));
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static Class<? extends Entity> parseClass( String data ) {
		Class<? extends Entity> entProto = null;
		try {
			Class<?> proto = Class.forName( data );
			entProto = proto.asSubclass(Entity.class);
		}
		catch (ClassNotFoundException e) {
			throw new InputErrorException( "Class not found " + data );
		}
		return entProto;
	}

	public static int parseInteger(String data)
	throws InputErrorException {
		return Input.parseInteger(data, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static int parseInteger(String data, int minValue, int maxValue)
	throws InputErrorException {
		int temp;
		try {
			temp = Integer.parseInt(data);
		}
		catch (NumberFormatException e) {
			throw new InputErrorException(INP_ERR_INTEGER, data);
		}

		if (temp < minValue || temp > maxValue)
			throw new InputErrorException(INP_ERR_INTEGERRANGE, minValue, maxValue, temp);

		return temp;
	}

	public static boolean isInteger(String val) {
		try {
			Integer.parseInt(val);
			return true;
		}
		catch (NumberFormatException e) { return false; }
	}

	public static IntegerVector parseIntegerVector(List<String> input, int minValue, int maxValue)
	throws InputErrorException {
		IntegerVector temp = new IntegerVector(input.size());

		for (int i = 0; i < input.size(); i++) {
			try {
				int element = Input.parseInteger(input.get(i), minValue, maxValue);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static IntegerVector parseIntegerVector(KeywordIndex kw, int minValue, int maxValue)
	throws InputErrorException {
		IntegerVector temp = new IntegerVector(kw.numArgs());

		for (int i = 0; i <kw.numArgs(); i++) {
			try {
				int element = Input.parseInteger(kw.getArg(i), minValue, maxValue);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static double parseTime(String data, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseTime(data, minValue, maxValue, 1.0);
	}

	/**
	 * Convert the given String to a double and apply the given conversion factor
	 */
	public static double parseTime(String data, double minValue, double maxValue, double factor )
	throws InputErrorException {
		double value = 0.0d;

		// check for hh:mm:ss or hh:mm
		if (data.indexOf(":") > -1) {
			String[] splitDouble = data.split( ":" );
			if (splitDouble.length != 2 && splitDouble.length != 3)
				throw new InputErrorException(INP_ERR_TIME, data);

			try {
				double hour = Double.valueOf(splitDouble[0]);
				double min = Double.valueOf(splitDouble[1]);
				double sec = 0.0d;

				if (splitDouble.length == 3)
					sec = Double.valueOf(splitDouble[2]);

				value = hour + (min / 60.0d) + (sec / 3600.0d);
			}
			catch (NumberFormatException e) {
				throw new InputErrorException(INP_ERR_TIME, data);
			}
		} else {
			value = Input.parseDouble(data);
		}
		value = value * factor;

		if (value < minValue || value > maxValue)
			throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, value);

		return value;
	}

	/**
	 * Convert the given String to a double and apply the given conversion factor
	 */
	public static double parseSeconds(String data, double minValue, double maxValue, double factor )
	throws InputErrorException {
		double value = 0.0d;

		// check for hh:mm:ss or hh:mm
		if (data.indexOf(":") > -1) {
			String[] splitDouble = data.split( ":" );
			if (splitDouble.length != 2 && splitDouble.length != 3)
				throw new InputErrorException(INP_ERR_TIME, data);

			try {
				double hour = Double.valueOf(splitDouble[0]);
				double min = Double.valueOf(splitDouble[1]);
				double sec = 0.0d;

				if (splitDouble.length == 3)
					sec = Double.valueOf(splitDouble[2]);

				value = hour * 3600.0d + min * 60.0d + sec;
			}
			catch (NumberFormatException e) {
				throw new InputErrorException(INP_ERR_TIME, data);
			}
		} else {
			value = Input.parseDouble(data);
			value = value * factor;
		}

		if (value < minValue || value > maxValue)
			throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, value);

		return value;
	}

	private static final Pattern is8601date = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	private static final Pattern is8601time = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}");
	private static final Pattern is8601full = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}\\.\\d{1,6}");
	private static final Pattern isextendtime = Pattern.compile("\\d{1,}:\\d{2}:\\d{2}");
	private static final Pattern isextendfull = Pattern.compile("\\d{1,}:\\d{2}:\\d{2}.\\d{1,6}");
	private static final long usPerSec = 1000000;
	private static final long usPerMin = 60 * usPerSec;
	private static final long usPerHr  = 60 * usPerMin;
	private static final long usPerDay = 24 * usPerHr;
	public static final long usPerYr  = 365 * usPerDay;
	/**
	 * Parse an RFC8601 date time and return it as an offset in microseconds from
	 * 0AD. This assumes a very simple concept of a 365 day year with no leap years
	 * and no leap seconds.
	 *
	 * An RFC8601 date time looks like YYYY-MM-DD HH:MM:SS.mmm or YYYY-MM-DDTHH:MM:SS.mmm
	 *
	 * @param input
	 * @param datumYear
	 * @return
	 */
	public static long parseRFC8601DateTime(String input) {
		if (is8601time.matcher(input).matches()) {
			int YY = Integer.parseInt(input.substring(0, 4));
			int MM = Integer.parseInt(input.substring(5, 7));
			int DD = Integer.parseInt(input.substring(8, 10));
			int hh = Integer.parseInt(input.substring(11, 13));
			int mm = Integer.parseInt(input.substring(14, 16));
			int ss = Integer.parseInt(input.substring(17, 19));
			return getUS(input, YY, MM, DD, hh, mm, ss, 0);
		}

		if (is8601full.matcher(input).matches()) {
			int YY = Integer.parseInt(input.substring(0, 4));
			int MM = Integer.parseInt(input.substring(5, 7));
			int DD = Integer.parseInt(input.substring(8, 10));
			int hh = Integer.parseInt(input.substring(11, 13));
			int mm = Integer.parseInt(input.substring(14, 16));
			int ss = Integer.parseInt(input.substring(17, 19));

			// grab the us values and zero-pad to a full 6-digit number
			String usChars =  input.substring(20, input.length());
			int us = 0;
			switch (usChars.length()) {
			case 1: us =  Integer.parseInt(usChars) * 100000; break;
			case 2: us =  Integer.parseInt(usChars) *  10000; break;
			case 3: us =  Integer.parseInt(usChars) *   1000; break;
			case 4: us =  Integer.parseInt(usChars) *    100; break;
			case 5: us =  Integer.parseInt(usChars) *     10; break;
			case 6: us =  Integer.parseInt(usChars) *      1; break;
			}
			return getUS(input, YY, MM, DD, hh, mm, ss, us);
		}

		if (is8601date.matcher(input).matches()) {
			int YY = Integer.parseInt(input.substring(0, 4));
			int MM = Integer.parseInt(input.substring(5, 7));
			int DD = Integer.parseInt(input.substring(8, 10));
			return getUS(input, YY, MM, DD, 0, 0, 0, 0);
		}

		if (isextendtime.matcher(input).matches()) {
			int len = input.length();
			int hh = Integer.parseInt(input.substring(0, len - 6));
			int mm = Integer.parseInt(input.substring(len - 5, len - 3));
			int ss = Integer.parseInt(input.substring(len - 2, len));

			if (mm < 0 || mm > 59 || ss < 0 || ss > 59)
				throw new InputErrorException(INP_ERR_BADDATE, input);

			long ret = 0;
			ret += hh * usPerHr;
			ret += mm * usPerMin;
			ret += ss * usPerSec;
			return ret;
		}

		if (isextendfull.matcher(input).matches()) {
			int len = input.indexOf(".");
			int hh = Integer.parseInt(input.substring(0, len - 6));
			int mm = Integer.parseInt(input.substring(len - 5, len - 3));
			int ss = Integer.parseInt(input.substring(len - 2, len));

			if (mm < 0 || mm > 59 || ss < 0 || ss > 59)
				throw new InputErrorException(INP_ERR_BADDATE, input);

			// grab the us values and zero-pad to a full 6-digit number
			String usChars =  input.substring(len + 1, input.length());
			int us = 0;
			switch (usChars.length()) {
			case 1: us =  Integer.parseInt(usChars) * 100000; break;
			case 2: us =  Integer.parseInt(usChars) *  10000; break;
			case 3: us =  Integer.parseInt(usChars) *   1000; break;
			case 4: us =  Integer.parseInt(usChars) *    100; break;
			case 5: us =  Integer.parseInt(usChars) *     10; break;
			case 6: us =  Integer.parseInt(usChars) *      1; break;
			}
			long ret = 0;
			ret += hh * usPerHr;
			ret += mm * usPerMin;
			ret += ss * usPerSec;
			ret += us;
			return ret;
		}

		throw new InputErrorException(INP_ERR_BADDATE, input);
	}

	private static final long getUS(String input, int YY, int MM, int DD, int hh, int mm, int ss, int us) {
		// Validate ranges
		if (MM <= 0 || MM > 12)
			throw new InputErrorException(INP_ERR_BADDATE, input);

		if (DD <= 0 || DD > Clock.getDaysInMonth(MM))
			throw new InputErrorException(INP_ERR_BADDATE, input);

		if (hh < 0 || hh > 23)
			throw new InputErrorException(INP_ERR_BADDATE, input);

		if (mm < 0 || mm > 59 || ss < 0 || ss > 59)
			throw new InputErrorException(INP_ERR_BADDATE, input);

		long ret = 0;
		ret += YY * usPerYr;
		ret += (Clock.getFirstDayOfMonth(MM) - 1) * usPerDay;
		ret += (DD - 1) * usPerDay;
		ret += hh * usPerHr;
		ret += mm * usPerMin;
		ret += ss * usPerSec;
		ret += us;

		return ret;
	}

	public static double parseDouble(String data)
	throws InputErrorException {
		return Input.parseDouble(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public static double parseDouble(String data, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseDouble(data, minValue, maxValue, 1.0);
	}

	/**
	 * Convert the given String to a double and apply the given conversion factor
	 */
	public static double parseDouble(String data, double minValue, double maxValue, double factor)
	throws InputErrorException {
		double temp;
		try {
			temp = Double.parseDouble(data) * factor;
		}
		catch (NumberFormatException e) {
			throw new InputErrorException(INP_ERR_DOUBLE, data);
		}

		if (temp < minValue || temp > maxValue)
			throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, temp);

		return temp;
	}

	/**
	 * Convert the given String to a double including a unit conversion, if necessary
	 */
	public static double parseDouble(List<String> input, double minValue, double maxValue, String defaultUnitString)
	throws InputErrorException {
		Input.assertCountRange(input, 1, 2);

		// Warn if the default unit is assumed by the input data
		if( input.size() == 1 && defaultUnitString.length() > 0 )
			InputAgent.logWarning( "Missing units.  Assuming %s.", defaultUnitString );

		// If there are two values, then assume the last one is a unit
		double conversionFactor = 1.0;
		if( input.size() == 2 ) {

			// Determine the units
			Unit unit = Input.parseUnits( input.get(1) );

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity( defaultUnitString.replaceAll("[()]", "").trim(), Unit.class );
			if( defaultUnit == null ) {
				throw new InputErrorException( "Could not determine default units " + defaultUnitString );
			}

			if (defaultUnit.getClass() != unit.getClass())
				throw new InputErrorException( "Cannot convert from %s to %s", defaultUnit.getName(), unit.getName());

			// Determine the conversion factor from units to default units
			conversionFactor = unit.getConversionFactorToUnit( defaultUnit );
		}

		// Parse and convert the value
		return Input.parseDouble( input.get(0), minValue, maxValue, conversionFactor);
	}

	/**
	 * Convert the given input to a DoubleVector and apply the given conversion factor
	 */
	public static DoubleVector parseDoubleVector(List<String> input, double minValue, double maxValue, double factor)
	throws InputErrorException {
		DoubleVector temp = new DoubleVector(input.size());

		for (int i = 0; i < input.size(); i++) {
			try {
				double element = Input.parseDouble(input.get(i), minValue, maxValue, factor);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	/**
	 * Convert the given input to a DoubleVector and apply the given conversion factor
	 */
	public static DoubleVector parseDoubles(KeywordIndex kw, double minValue, double maxValue, Class<? extends Unit> unitType)
	throws InputErrorException {
		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		double factor = 1.0d;
		int numDoubles = kw.numArgs();

		// If not a Dimensionless value, a unit is mandatory
		if (unitType != DimensionlessUnit.class) {
			Entity ent = Entity.getNamedEntity(kw.getArg(kw.numArgs() - 1));
			if (ent == null)
				throw new InputErrorException(INP_ERR_NOUNITFOUND, kw.getArg(kw.numArgs() - 1), unitType.getSimpleName());

			Unit unit = Input.castEntity(ent, unitType);
			if (unit == null)
				throw new InputErrorException(INP_ERR_ENTCLASS, unitType.getSimpleName(), ent.getInputName(), ent.getClass().getSimpleName());

			factor = unit.getConversionFactorToSI();
			numDoubles = kw.numArgs() - 1;
		}

		DoubleVector temp = new DoubleVector(numDoubles);
		for (int i = 0; i < numDoubles; i++) {
			try {
				// Allow a special syntax for time-based inputs
				if (unitType == TimeUnit.class) {
					double element = Input.parseSeconds(kw.getArg(i), minValue, maxValue, factor);
					temp.add(element);
				}
				else {
					double element = Input.parseDouble(kw.getArg(i), minValue, maxValue, factor);
					temp.add(element);
				}
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	/**
	 * Convert the given input to a DoubleVector and apply the given conversion factor
	 */
	public static DoubleVector parseDoubles(List<String> input, double minValue, double maxValue, Class<? extends Unit> unitType)
	throws InputErrorException {
		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		double factor = 1.0d;
		int numDoubles = input.size();

		// If not a Dimensionless value, a unit is mandatory
		if (unitType != DimensionlessUnit.class) {
			Entity ent = Entity.getNamedEntity(input.get(input.size() - 1));
			if (ent == null)
				throw new InputErrorException(INP_ERR_NOUNITFOUND, input.get(input.size() - 1), unitType.getSimpleName());

			Unit unit = Input.castEntity(ent, unitType);
			if (unit == null)
				throw new InputErrorException(INP_ERR_ENTCLASS, unitType.getSimpleName(), ent.getInputName(), ent.getClass().getSimpleName());

			factor = unit.getConversionFactorToSI();
			numDoubles = input.size() - 1;
		}

		DoubleVector temp = new DoubleVector(numDoubles);
		for (int i = 0; i < numDoubles; i++) {
			try {
				// Allow a special syntax for time-based inputs
				if (unitType == TimeUnit.class) {
					double element = Input.parseSeconds(input.get(i), minValue, maxValue, factor);
					temp.add(element);
				}
				else {
					double element = Input.parseDouble(input.get(i), minValue, maxValue, factor);
					temp.add(element);
				}
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	/**
	 * Convert the given input to a DoubleVector including a unit conversion, if necessary
	 */
	public static DoubleVector parseDoubleVector(List<String> data, double minValue, double maxValue, String defaultUnitString)
	throws InputErrorException {
		// If there is more than one value, and the last one is not a number, then assume it is a unit
		String unitString = data.get( data.size()-1 );
		if( data.size() > 1 && !Tester.isDouble(unitString) ) {

			// Determine the units
			Unit unit = Input.parseUnits(unitString);

			// Determine the default units
			Unit defaultUnit = Input.tryParseEntity( defaultUnitString, Unit.class );
			if( defaultUnit == null ) {
				throw new InputErrorException( "Could not determine default units " + defaultUnitString );
			}

			if (defaultUnit.getClass() != unit.getClass())
				throw new InputErrorException( "Cannot convert from %s to %s", defaultUnit.getName(), unit.getName());

			// Determine the conversion factor to the default units
			double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

			// grab all but the final argument (the unit)
			ArrayList<String> numericData = new ArrayList<String>(data.size() - 1);
			for (int i = 0; i < data.size() -1; i++)
				numericData.add(data.get(i));

			return Input.parseDoubleVector( numericData, minValue, maxValue, conversionFactor);
		}
		else {
			if( defaultUnitString.length() > 0 )
				InputAgent.logWarning( "Missing units.  Assuming %s.", defaultUnitString );
		}

		// Parse and convert the values
		return Input.parseDoubleVector( data, minValue, maxValue, 1.0d);
	}

	public static String parseString(String input, ArrayList<String> validList)
	throws InputErrorException {
		return parseString(input, validList, false);
	}

	public static String parseString(String input, ArrayList<String> validList, boolean caseSensitive)
	throws InputErrorException {
		for (String valid : validList) {
			if (caseSensitive && valid.equals(input))
				return valid;

			if (!caseSensitive && valid.equalsIgnoreCase(input))
				return valid;
		}

		throw new InputErrorException(INP_ERR_BADCHOICE, validList.toString(), input);
	}

	public static ArrayList<String> parseStrings(KeywordIndex kw, ArrayList<String> validList, boolean caseSensitive)
	throws InputErrorException {
		ArrayList<String> temp = new ArrayList<String>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			try {
				String element = Input.parseString(kw.getArg(i), validList);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static <T extends Enum<T>> T parseEnum(Class<T> aClass, String input) {
		try {
			return Enum.valueOf(aClass, input);
		} catch (IllegalArgumentException e) {
			throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), input);
		} catch (NullPointerException e) {
			throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), input);
		}
	}

	public static Class<? extends Entity> parseEntityType(String input)
	throws InputErrorException {
		ObjectType type = Input.tryParseEntity( input, ObjectType.class );
		if (type == null)
			throw new InputErrorException("Entity type not found: %s", input);

		Class<? extends Entity> klass = type.getJavaClass();
		if (klass == null)
			throw new InputErrorException("ObjectType %s does not have a java class set", input);

		return klass;
	}

	public static void assertUnitsMatch(Class<? extends Unit> u1, Class<? extends Unit> u2)
	throws InputErrorException {
		if (u1 != u2)
			throw new InputErrorException(INP_ERR_UNITS);
	}

	public static <T extends Entity> Class<? extends T> checkCast(Class<? extends Entity> klass, Class<T> parent) {
		try {
			return klass.asSubclass(parent);
		}
		catch (ClassCastException e) {
			throw new InputErrorException(INP_ERR_NOTSUBCLASS, parent.getName(), klass.getName());
		}
	}

	public static <T> T castImplements(Entity ent, Class<T> klass)
	throws InputErrorException {
		try {
			return klass.cast(ent);
		}
		catch (ClassCastException e) {
			throw new InputErrorException(INP_ERR_INTERFACE, klass.getName(), ent.getInputName());
		}
	}

	private static <T extends Entity> T castEntity(Entity ent, Class<T> aClass)
	throws InputErrorException {
		try {
			return aClass.cast(ent);
		}
		catch (ClassCastException e) {
			return null;
		}
	}

	public static <T extends Entity> T parseEntity(String choice, Class<T> aClass)
	throws InputErrorException {
		Entity ent = Entity.getNamedEntity(choice);
		if (ent == null) {
			throw new InputErrorException(INP_ERR_ENTNAME, choice);
		}
		T t = Input.castEntity(ent, aClass);
		if (t == null) {
			throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), choice, ent.getClass().getSimpleName());
		}
		return t;
	}

	public static <T extends Entity> T tryParseEntity(String choice, Class<T> aClass) {
		return Input.castEntity(Entity.getNamedEntity(choice), aClass);
	}

	public static <T extends Entity> ArrayList<T> parseEntityList(KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<T>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			Entity ent = Entity.getNamedEntity(kw.getArg(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, kw.getArg(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group) {
				ArrayList<Entity> gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castEntity(gList.get(j), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), gList.get(j).getClass().getSimpleName());
					}
					temp.add(t);
				}
			} else {
				T t = Input.castEntity(ent, aClass);
				if (t == null) {
					throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), kw.getArg(i), ent.getClass().getSimpleName());
				}
				temp.add(t);
			}
		}

		if (unique)
			Input.assertUnique(temp);

		return temp;
	}


	public static <T extends Entity> ArrayList<T> parseEntityList(List<String> input, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<T>(input.size());

		for (int i = 0; i < input.size(); i++) {
			Entity ent = Entity.getNamedEntity(input.get(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, input.get(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group) {
				ArrayList<Entity> gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castEntity(gList.get(j), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), gList.get(j).getClass().getSimpleName());
					}
					temp.add(t);
				}
			} else {
				T t = Input.castEntity(ent, aClass);
				if (t == null) {
					throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), input.get(i), ent.getClass().getSimpleName());
				}
				temp.add(t);
			}
		}

		if (unique)
			Input.assertUnique(temp);

		return temp;
	}


	public static <T extends Entity> ArrayList<ArrayList<T>> parseListOfEntityLists(KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<ArrayList<T>> temp = new ArrayList<ArrayList<T>>(subArgs.size());

		for (int i = 0; i < subArgs.size(); i++) {
			try {
				ArrayList<T> element = Input.parseEntityList(subArgs.get(i), aClass, unique);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static <T> ArrayList<T> parseInterfaceEntityList(KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<T>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			Entity ent = Entity.getNamedEntity(kw.getArg(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, kw.getArg(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group) {
				ArrayList<Entity> gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castImplements(gList.get(j), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), gList.get(j).getClass().getSimpleName());
					}
					temp.add(t);
				}
			} else {
				T t = Input.castImplements(ent, aClass);
				if (t == null) {
					throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), kw.getArg(i), ent.getClass().getSimpleName());
				}
				temp.add(t);
			}
		}

		if (unique)
			Input.assertUniqueInterface(temp);

		return temp;
	}

	public static Color4d parseColour(KeywordIndex kw) {

		Input.assertCount(kw, 1, 3);

		// Color names
		if (kw.numArgs() == 1) {
			Color4d colAtt = ColourInput.getColorWithName(kw.getArg(0).toLowerCase());
			if( colAtt == null ) {
				throw new InputErrorException( "Color " + kw.getArg( 0 ) + " not found" );
			}
			else {
				return colAtt;
			}
		}

		// RGB
		else {
			DoubleVector dbuf = Input.parseDoubles(kw, 0.0d, 255.0d, DimensionlessUnit.class);
			double r = dbuf.get(0);
			double g = dbuf.get(1);
			double b = dbuf.get(2);

			if (r > 1.0f || g > 1.0f || b > 1.0f) {
				r /= 255.0d;
				g /= 255.0d;
				b /= 255.0d;
			}

			return new Color4d(r, g, b);
		}
	}

	private static void assertUnique(ArrayList<? extends Entity> list) {
		for (int i = 0; i < list.size(); i++) {
			Entity ent = list.get(i);
			for (int j = i + 1; j < list.size(); j++) {
				if (ent == list.get(j)) {
					throw new InputErrorException(INP_ERR_NOTUNIQUE, ent.getName());
				}
			}
		}
	}

	private static void assertUniqueInterface(ArrayList<?> list) {
		for (int i = 0; i < list.size(); i++) {
			Entity ent = (Entity)list.get(i);
			for (int j = i + 1; j < list.size(); j++) {
				if (ent == list.get(j)) {
					throw new InputErrorException(INP_ERR_NOTUNIQUE, ent.getName());
				}
			}
		}
	}

	public static void validateIndexedLists(ListInput<?> keys, ListInput<?> vals)
	throws InputErrorException {
		// If no values set, no validation to be done
		if (vals.getValue() == null)
			return;

		// values are set but indexed list has not
		if (keys.getValue() == null)
			throw new InputErrorException(INP_VAL_LISTSET, keys.getKeyword(), vals.getKeyword());

		// Both are set, but of differing size
		if (keys.getListSize() != vals.getListSize())
			throw new InputErrorException(INP_VAL_LISTSIZE, keys.getKeyword(), vals.getKeyword());
	}

	public static void validateInputSize(ListInput<?> list1, ListInput<?> list2)
	throws InputErrorException {
		// One list is set but not the other
		if (list1.getValue() != null && list2.getValue() == null)
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword());

		if (list1.getValue() == null && list2.getValue() != null)
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword());

		// Both are set, but of differing size
		if (list1.getListSize() != list2.getListSize())
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword() );
	}

	public static void validateIndexedLists(ArrayList<?> keys, DoubleVector values, String keyName, String valueName)
	throws InputErrorException {
		// If no values set, no validation to be done
		if (values == null)
			return;

		// values are set but indexed list has not
		if (keys == null)
			throw new InputErrorException(INP_VAL_LISTSET, valueName, keyName);

		// Both are set, but of differing size
		if (keys.size() != values.size())
			throw new InputErrorException(INP_VAL_LISTSIZE, keyName, valueName);
	}

	/**
	 * Returns a list of valid options if the input has limited number of
	 * choices (e.g TRUE or FALSE for BooleanInput).
	 * <p>
	 * This method must be overridden for an input to be shown with a drop-down
	 * menu in the Input Editor.
	 */
	public ArrayList<String> getValidOptions() {
		return null;
	}

	public static Unit parseUnits(String str) {
		try {
			return Input.parseEntity(str, Unit.class);
		}
		catch(InputErrorException ex) {
			throw new InputErrorException(String.format("Could not find a unit named: %s", str));
		}
	}

	public String getDefaultStringForKeyInputs(String unitString) {

		if (defValue == null)
			return NO_VALUE;

		if (defValue.getClass() == Boolean.class) {
			if((Boolean)defValue)
				return "TRUE";

			return "FALSE";
		}

		StringBuilder tmp = new StringBuilder();
		if (defValue.getClass() == Double.class ||
		   defValue.getClass() == Integer.class ||
		   Entity.class.isAssignableFrom(Double.class)) {
			if (defValue.equals(Integer.MAX_VALUE) || defValue.equals(Double.POSITIVE_INFINITY))
				return POSITIVE_INFINITY;

			if (defValue.equals(Integer.MIN_VALUE) || defValue.equals(Double.NEGATIVE_INFINITY))
				return NEGATIVE_INFINITY;

			tmp.append(defValue);
		} else if (defValue.getClass() == SampleConstant.class ) {
			return defValue.toString();
		} else if (defValue.getClass() == DoubleVector.class) {
			DoubleVector def = (DoubleVector)defValue;
			if (def.size() == 0)
				return NO_VALUE;

			tmp.append(def.get(0));
			for (int i = 1; i < def.size(); i++) {
				tmp.append(SEPARATOR);
				tmp.append(def.get(i));
			}
		} else if ( Entity.class.isAssignableFrom( defValue.getClass() ) ) {
			tmp.append(((Entity)defValue).getInputName());
		}
		else {
			return "?????";
		}

		if (unitString == null || !unitString.isEmpty()) {
			tmp.append(SEPARATOR);
			tmp.append(unitString);
		}
		return tmp.toString();
	}
}
