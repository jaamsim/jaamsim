/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;

/**
 * OutputHandle is a class that represents all the useful runtime information for an output,
 * specifically a reference to the runtime annotation and the method it points to
 * @author matt.chudleigh
 *
 */
public class OutputHandle {

	public Entity ent;
	public OutputPair pair;
	public Class<? extends Unit> unitType;

	private static final HashMap<Class<? extends Entity>, ArrayList<OutputPair>> outputPairCache;

	static {
		outputPairCache = new HashMap<Class<? extends Entity>, ArrayList<OutputPair>>();
	}

	public OutputHandle(Entity e, String outputName) {
		ent = e;
		pair = OutputHandle.getOutputPair(e.getClass(), outputName);
		unitType = pair.annotation.unitType();
	}

	protected OutputHandle(Entity e) {
		ent = e;
	}

	private static class OutputPair {
		public Method method;
		public Output annotation;

		public OutputPair(Method m, Output a) {
			method = m;
			annotation = a;
		}
	}

	// Note: this method will not include attributes in the list. For a complete list use
	// Entity.hasOutput()
	public static Boolean hasOutput(Class<? extends Entity> klass, String outputName) {
		return OutputHandle.getOutputPair(klass, outputName) != null;
	}

	private static OutputPair getOutputPair(Class<? extends Entity> klass, String outputName) {
		for (OutputPair p : getOutputPair(klass)) {
			if( p.annotation.name().equals(outputName) )
				return p;
		}
		return null;
	}

	private static ArrayList<OutputPair> getOutputPair(Class<? extends Entity> klass) {
		ArrayList<OutputPair> ret = outputPairCache.get(klass);
		if (ret != null)
			return ret;

		// klass has not been cached yet, generate pairs
		ret = new ArrayList<OutputPair>();
		for (Method m : klass.getMethods()) {
			Output a = m.getAnnotation(Output.class);
			if (a == null)
				continue;

			// Check that this method only takes a single double (simTime) parameter
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
			    paramTypes[0] != double.class) {
				continue;
			}

			ret.add(new OutputPair(m, a));
		}
		outputPairCache.put(klass, ret);
		return ret;
	}

	/**
	 * Return a list of the OuputHandles for the given entity.
	 * @param e = the entity whose OutputHandles are to be returned.
	 * @return = ArrayList of OutputHandles.
	 */
	public static ArrayList<OutputHandle> getOutputHandleList(Entity e) {
		Class<? extends Entity> klass = e.getClass();
		ArrayList<OutputPair> list = getOutputPair(klass);
		ArrayList<OutputHandle> ret = new ArrayList<OutputHandle>(list.size());
		for( OutputPair p : list ) {
			//ret.add( new OutputHandle(e, p) );
			ret.add( e.getOutputHandle(p.annotation.name()) );  // required to get the correct unit type for the output
		}

		// And the attributes
		for (String attribName : e.getAttributeNames()) {
			ret.add(e.getOutputHandle(attribName));
		}

		Collections.sort(ret, new OutputHandleComparator());
		return ret;
	}

	private static class OutputHandleComparator implements Comparator<OutputHandle> {

		@Override
		public int compare(OutputHandle hand0, OutputHandle hand1) {
			Class<?> class0 = hand0.getDeclaringClass();
			Class<?> class1 = hand1.getDeclaringClass();

			if (class0 == class1)
				return 0;

			if (class0.isAssignableFrom(class1))
				return -1;
			else
				return 1;
		}
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getValue(double simTime, Class<T> klass) {
		if( pair.method == null )
			return null;

		T ret = null;
		try {
			if (!klass.isAssignableFrom(pair.method.getReturnType()))
				return null;

			ret = (T)pair.method.invoke(ent, simTime);
		}
		catch (InvocationTargetException ex) {}
		catch (IllegalAccessException ex) {}
		catch (ClassCastException ex) {}
		return ret;
	}

	public boolean isNumericValue() {
		Class<?> rtype = this.getReturnType();
		if (rtype == Double.class) return true;
		if (rtype == double.class) return true;
		if (rtype == Float.class) return true;
		if (rtype == float.class) return true;
		if (rtype == Long.class) return true;
		if (rtype == long.class) return true;
		if (rtype == Integer.class) return true;
		if (rtype == int.class) return true;
		if (rtype == Short.class) return true;
		if (rtype == short.class) return true;
		if (rtype == Character.class) return true;
		if (rtype == char.class) return true;

		return false;
	}

	/**
	 * Checks the output for all possible numerical types and returns a double representing the value
	 * @param simTime
	 * @param def - the default value if the return is null or not a number value
	 * @return
	 */
	public double getValueAsDouble(double simTime, double def, Unit u) {
		double ret = getValueAsDouble(simTime, def);
		Class<? extends Unit> ut = this.getUnitType();
		if (u == null)
			return ret;

		if (u.getClass() != ut)
			throw new ErrorException("Unit Mismatch");

		ret /= u.getConversionFactorToSI();
		return ret;
	}

	/**
	 * Checks the output for all possible numerical types and returns a double representing the value
	 * @param simTime
	 * @param def - the default value if the return is null or not a number value
	 * @return
	 */
	public double getValueAsDouble(double simTime, double def) {
		Class<?> retType = this.getReturnType();
		if (retType == double.class)
			return this.getValue(simTime, double.class);

		if (retType == Double.class) {
			Double val = getValue(simTime, Double.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Float.class) {
			Float val = getValue(simTime, Float.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Long.class) {
			Long val = getValue(simTime, Long.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Integer.class) {
			Integer val = getValue(simTime, Integer.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Short.class) {
			Short val = getValue(simTime, Short.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Character.class) {
			Character val = getValue(simTime, Character.class);
			if (val == null) return def;
			return val.charValue();
		}

		if (retType == float.class)
			return this.getValue(simTime, float.class);
		if (retType == int.class)
			return this.getValue(simTime, int.class);
		if (retType == long.class)
			return this.getValue(simTime, long.class);
		if (retType == short.class)
			return this.getValue(simTime, short.class);
		if (retType == char.class)
			return this.getValue(simTime, char.class);

		return def;
	}

	public Class<?> getReturnType() {
		assert (pair.method != null);
		return pair.method.getReturnType();
	}

	public Class<?> getDeclaringClass() {
		assert (pair.method != null);
		return pair.method.getDeclaringClass();
	}

	public void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	public String getDescription() {
		assert (pair.annotation != null);
		return pair.annotation.description();
	}

	public String getName() {
		assert (pair.annotation != null);
		return pair.annotation.name();
	}

}
