/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2011 Ausenco Engineering Canada Inc.
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
import java.util.ArrayList;
import java.util.Arrays;

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation3D.InputAgent;
import com.sandwell.JavaSimulation3D.util.Shape;

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
	protected static final String INP_ERR_NOTUNIQUE = "List must contain unique entries, repeated entry: %s";
	protected static final String INP_ERR_NOTVALIDENTRY = "List must not contain: %s";
	protected static final String INP_ERR_ENTCLASS = "Expected a %s, %s is a %s";
	protected static final String INP_VAL_LISTSET = "Values found for %s without %s being set";
	protected static final String INP_VAL_LISTSIZE = "%s and %s must be of equal size";

	private final String keyword; // the preferred name for the input keyword
	private final String category;

	protected T defValue;
	protected T value;

	protected String unitString;
	private boolean appendable; // indicates if input is appendable
	private boolean locked; // indicates if input is locked for this entity
	private boolean edited; // indicates if input has been edited for this entity
	protected String valueString; // value from .cfg file
	protected String editedValueString; // new value from edit box

	public Input(String key, String cat, T def) {
		this(key, cat, "", def);
	}

	public Input(String key, String cat, String units, T def) {
		keyword = key;
		category = cat;

		setDefaultValue(def);

		unitString = units;

		appendable = false;
		locked = false;
		edited = false;
		valueString = "";
		editedValueString = "";
	}

	public void reset() {
		this.setDefaultValue( this.getDefaultValue() );
		valueString = "";
		editedValueString = "";
		edited = false;
	}

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

	public T getValue() {
		return value;
	}

	public void setUnits(String units) {
		unitString = units;
	}

	public String getUnits() {
		return unitString;
	}

	public void setAppendable(boolean bool) {
		appendable = bool;
	}

	public boolean isAppendable() {
		return appendable;
	}

	public void setLocked(boolean bool) {
		locked = bool;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setEdited(boolean bool) {
		edited = bool;
	}

	public boolean isEdited() {
		return edited;
	}

	public String getValueString() {
		if(edited) {
			return editedValueString;
		}
		return valueString;
	}

	public void setValueString(String str) {
		valueString = str;
	}

	public void setEditedValueString(String str) {
		editedValueString = str;
	}

	public abstract void parse(StringVector input) throws InputErrorException;

	public static void assertCount(StringVector input, int... counts)
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

	public static void assertCountRange(StringVector input, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(input, min);
			return;
		}

		if (input.size() < min || input.size() > max)
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, input.toString());
	}

	public static void assertCountEven( StringVector input )
	throws InputErrorException {
		if ( ( input.size() % 2 ) != 0 )
			throw new InputErrorException(INP_ERR_EVENCOUNT, input.toString());
	}

	public static void assertCountOdd( StringVector input )
	throws InputErrorException {
		if ( ( input.size() % 2 ) == 0 )
			throw new InputErrorException(INP_ERR_ODDCOUNT, input.toString());
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

	public static <T> T parse(StringVector data, Class<T> aClass, String units, double minValue, double maxValue) {

		if( aClass == Double.class ) {
			Input.assertCount(data, 1, 2);
			Double value;

			// If there are 2 entries, assume the last entry is a unit
			if( data.size() == 2 ) {

				// Determine the units
				Unit unit = Input.parseEntity( data.get( data.size()- 1), Unit.class );

				// Determine the default units
				Unit defaultUnit = Input.tryParseEntity( units.replaceAll("[()]", "").trim(), Unit.class );
				if( defaultUnit == null ) {
					throw new InputErrorException( "Could not determine default units " + units );
				}

				// Determine the conversion factor to the default units
				double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

				// Parse and convert the values
				value = Input.parseDouble(data.get( data.size()-2 ), minValue, maxValue, conversionFactor);
			}
			else {
				// Parse the values
				value = Input.parseDouble(data.get( data.size()-1 ), minValue, maxValue);

				if( units.length() > 0 )
					InputAgent.logWarning( "Missing units.  Assuming %s.", units );
			}
			return aClass.cast(value);
		}

		if( aClass == DoubleVector.class ) {
			DoubleVector value;

			// If there is more than one value, and the last one is not a number, then assume it is a unit
			if( data.size() > 1 && !Tester.isDouble( data.get( data.size()-1 ) ) ) {

				// Determine the units
				Unit unit = Input.parseEntity( data.get( data.size()- 1), Unit.class );

				// Determine the default units
				Unit defaultUnit = Input.tryParseEntity( units.replaceAll("[()]", "").trim(), Unit.class );
				if( defaultUnit == null ) {
					throw new InputErrorException( "Could not determine default units " + units );
				}

				// Determine the conversion factor to the default units
				double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

				// Parse and convert the values
				value = Input.parseDoubleVector(data.subString(0,data.size()-2), minValue, maxValue, conversionFactor);
			}
			else {
				// Parse the values
				value = Input.parseDoubleVector(data.subString(0,data.size()-1), minValue, maxValue);

				if( units.length() > 0 )
					InputAgent.logWarning( "Missing units.  Assuming %s.", units );
			}
			return aClass.cast( value );
		}

		if( Entity.class.isAssignableFrom(aClass) ) {
			Class<? extends Entity> temp = aClass.asSubclass(Entity.class);
			Input.assertCount(data, 1, 1);
			return aClass.cast( Input.parseEntity(data.get(0), temp) );
		}

		if( aClass == TimeValue.class ) {

			TimeValue value;
			Input.assertCount(data, 1, 2, 12, 13);

			// If there are 2 or 13 entries, assume the last entry is a unit
			if( data.size() == 2 || data.size() == 13 ) {

				// Determine the units
				Unit unit = Input.parseEntity( data.get( data.size()- 1), Unit.class );

				// Determine the default units
				Unit defaultUnit = Input.tryParseEntity( units.replaceAll("[()]", "").trim(), Unit.class );
				if( defaultUnit == null ) {
					throw new InputErrorException( "Could not determine default units " + units );
				}

				// Determine the conversion factor to the default units
				double conversionFactor = unit.getConversionFactorToUnit( defaultUnit );

				// Parse and convert the values
				StringVector temp = data.subString(0,data.size()-2);
				value = Input.parseTimeValue(temp, minValue, maxValue, conversionFactor);
			}
			else {
				// Parse the values
				value = Input.parseTimeValue(data, minValue, maxValue);

				if( units.length() > 0 )
					InputAgent.logWarning( "Missing units.  Assuming %s.", units );
			}
			return aClass.cast( value );
		}

		// TODO - parse other classes
//		if( aClass == Integer.class ) {
//		}
//
//		if( aClass == IntegerVector.class ) {
//		}
		return null;
	}

	public static boolean parseBoolean(String data)
	throws InputErrorException {
		String temp = data.toUpperCase();
		if (temp.equals("TRUE") || temp.equals("1")) {
			return true;
		}

		if (temp.equals("FALSE") || temp.equals("0")) {
			return false;
		}

		throw new InputErrorException(INP_ERR_BOOLEAN, data);
	}

	public static BooleanVector parseBooleanVector(StringVector input)
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

	public static ArrayList<ColoringAttributes> parseColorVector(StringVector input)
	throws InputErrorException {
		ArrayList<ColoringAttributes> temp = new ArrayList<ColoringAttributes>(input.size());

		ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(input);
		for (int i = 0; i < splitData.size(); i++) {
			try {
				ColoringAttributes element = Input.parseColour(splitData.get(i));
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

	protected static boolean isInteger(String val) {
		try {
			Integer.parseInt(val);
			return true;
		}
		catch (NumberFormatException e) { return false; }
	}

	public static IntegerVector parseIntegerVector(StringVector input, int minValue, int maxValue)
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

	public static DoubleVector parseTimeVector(StringVector input, double minValue, double maxValue)
	throws InputErrorException {
		DoubleVector temp = new DoubleVector(input.size());

		for (int i = 0; i < input.size(); i++) {
			try {
				double element = Input.parseTime(input.get(i), minValue, maxValue);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static TimeValue parseTimeValue(StringVector input, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseTimeValue(input, minValue, maxValue, 1.0);
	}

	public static TimeValue parseTimeValue(StringVector input, double minValue, double maxValue, double factor)
	throws InputErrorException {
		Input.assertCount(input, 1, 12);

		if (input.size() == 12) {
			try {
				return new TimeValue(Input.parseDoubleVector(input, minValue, maxValue, factor));
			} catch (InputErrorException e) {}
			try {
				return new TimeValue(Input.parseEntityList(input, ProbabilityDistribution.class, false));
			} catch (InputErrorException e) {}
		}

		// Attempt to parse as a double, but catch the error
		try {
			return new TimeValue(Input.parseTime(input.get(0), minValue, maxValue, factor));
		} catch (InputErrorException e) {}

		try {
			return new TimeValue(Input.parseEntity(input.get(0), ProbabilityDistribution.class));
		} catch (InputErrorException e) {}

		throw new InputErrorException(INP_ERR_TIMEVALUE, input.toString());
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

	public static DoubleVector parseDoubleVector(StringVector input, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseDoubleVector(input, minValue, maxValue, 1.0);
	}

	/**
	 * Convert the given StringVector to a DoubleVector and apply the given conversion factor
	 */
	public static DoubleVector parseDoubleVector(StringVector input, double minValue, double maxValue, double factor)
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

	public static Vector3d parseVector3d(StringVector input)
	throws InputErrorException {
		return Input.parseVector3d(input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public static Vector3d parseVector3d(StringVector input, double min, double max)
	throws InputErrorException {
		Input.assertCountRange(input, 1, 3);
		DoubleVector temp = Input.parseDoubleVector(input, min, max);

		Vector3d ret = new Vector3d();

		ret.x = temp.get(0);
		if (temp.size() > 1)
			ret.y = temp.get(1);

		if (temp.size() > 2)
			ret.z = temp.get(2);

		return ret;
	}

	public static String parseString(String input, ArrayList<String> validList)
	throws InputErrorException {
		for (String valid : validList) {
			if (valid.equalsIgnoreCase(input))
				return valid;
		}

		throw new InputErrorException(INP_ERR_BADCHOICE, validList.toString(), input);
	}

	public static ArrayList<String> parseStringVector(StringVector input, ArrayList<String> validList)
	throws InputErrorException {
		ArrayList<String> temp = new ArrayList<String>(input.size());

		for (int i = 0; i < input.size(); i++) {
			try {
				String element = Input.parseString(input.get(i), validList);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static Class<? extends Entity> parseEntityType(String input)
	throws InputErrorException {
		ObjectType type = Input.tryParseEntity( input, ObjectType.class );
		if( type != null ) {
			try {
				Class<?> proto = Class.forName( type.getJavaClass().getName() );
				Class<? extends Entity> entProto = proto.asSubclass(Entity.class);
				return entProto;
			}
			catch (ClassNotFoundException e) {} // Keep trying other classes
			catch (ClassCastException e) {} //
		}

		throw new InputErrorException("Entity type not found: %s", input);
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

	public static <T extends Entity> ArrayList<T> parseEntityList(StringVector input, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<T>(input.size());

		for (int i = 0; i < input.size(); i++) {
			Entity ent = Entity.getNamedEntity(input.get(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, input.get(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group) {
				Vector gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castEntity(((Entity)gList.get(j)), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), ((Entity)gList.get(j)).getClass().getSimpleName());
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


	public static <T extends Entity> ArrayList<ArrayList<T>> parseListOfEntityLists(StringVector input, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<ArrayList<T>> temp = new ArrayList<ArrayList<T>>( input.size() );

		ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(input);
		for (int i = 0; i < splitData.size(); i++) {
			try {
				ArrayList<T> element = Input.parseEntityList(splitData.get(i), aClass, unique);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		return temp;
	}

	public static ColoringAttributes parseColour( StringVector input ) {

		Input.assertCount(input, 1, 3);

		// Color names
		if( input.size() == 1 ) {
			ColoringAttributes colAtt = Shape.getColorWithName( input.get( 0 ) );
			if( colAtt == null ) {
				throw new InputErrorException( "Color " + input.get( 0 ) + " not found" );
			}
			else {
				return colAtt;
			}
		}

		// RGB
		else {
			DoubleVector dbuf = Input.parseDoubleVector(input, 0.0d, 255);;
			return Shape.defineColor((float)dbuf.get(0), (float)dbuf.get(1), (float)dbuf.get(2));
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
	public static void validateIndexedLists(ArrayList<? extends Entity> keys, BooleanVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(ArrayList<? extends Entity> keys, IntegerVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(ArrayList<? extends Entity> keys, DoubleVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(DoubleVector keys, DoubleVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(DoubleVector keys, BooleanVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(IntegerVector keys, IntegerVector values, String keyName, String valueName)
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

	public static void validateIndexedLists(DoubleVector keys, ArrayList<ColoringAttributes> values, String keyName, String valueName)
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

	public static void validateIndexedLists(ArrayList<?> keys, ArrayList<?> values, String keyName, String valueName)
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

	public static void validateInputSize(DoubleListInput list1, DoubleListInput list2)
	throws InputErrorException {

		// One list is set but not the other
		if( list1.getValue() != null && list2.getValue() == null )
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword() );

		if( list1.getValue() == null && list2.getValue() != null )
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword() );

		// Both are set, but of differing size
		if (list1.getValue().size() != list2.getValue().size())
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword() );

	}

	void updateEditingFlags() {

		// Keyword is edited
		if(InputAgent.hasAddedRecords() ||
				Entity.simulation.getSimulationState() >= Simulation.SIM_STATE_CONFIGURED) {
			this.setEdited(true);
		}
	}
}
