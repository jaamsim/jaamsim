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

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;

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

	private static HashMap<Class<? extends Entity>, ArrayList<OutputPair>> outputPairCache = null;

	public OutputHandle(Entity e, String outputName) {
		ent = e;
		pair = OutputHandle.getOutputPair(e.getClass(), outputName);
		unitType = pair.annotation.unitType();
	}

	private static class OutputPair {
		public Method method;
		public Output annotation;

		public OutputPair(Method m, Output a) {
			method = m;
			annotation = a;
		}
	}

	public static Boolean hasOutput(Class<? extends Entity> klass, String outputName) {
		return OutputHandle.getOutputPair(klass, outputName) != null;
	}

	private static OutputPair getOutputPair(Class<? extends Entity> klass, String outputName) {
		if( outputPairCache == null )
			outputPairCache = new HashMap<Class<? extends Entity>, ArrayList<OutputPair>>();

		if( ! outputPairCache.containsKey(klass) )
			OutputHandle.buildOutputPairCache(klass);

		for( OutputPair p : outputPairCache.get(klass) ) {
			if( p.annotation.name().equals(outputName) )
				return p;
		}
		return null;
	}

	private static void buildOutputPairCache(Class<? extends Entity> klass) {
		ArrayList<OutputPair> list = new ArrayList<OutputPair>();
		for (Method m : klass.getMethods()) {
			Output a = m.getAnnotation(Output.class);
			if (a == null) {
				continue;
			}

			// Check that this method only takes a single double (simTime) parameter
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
				paramTypes[0] != double.class) {
				continue;
			}

			list.add( new OutputPair(m,a) );
		}
		outputPairCache.put(klass, list);
	}

	/**
	 * Return a list of the OuputHandles for the given entity.
	 * @param e = the entity whose OutputHandles are to be returned.
	 * @return = ArrayList of OutputHandles.
	 */
	public static ArrayList<OutputHandle> getOutputHandleList(Entity e) {
		if( outputPairCache == null )
			outputPairCache = new HashMap<Class<? extends Entity>, ArrayList<OutputPair>>();

		Class<? extends Entity> klass = e.getClass();
		if( ! outputPairCache.containsKey(klass) )
			OutputHandle.buildOutputPairCache(klass);

		ArrayList<OutputPair> list = outputPairCache.get(klass);
		ArrayList<OutputHandle> ret = new ArrayList<OutputHandle>(list.size());
		for( OutputPair p : list ) {
			//ret.add( new OutputHandle(e, p) );
			ret.add( e.getOutputHandle(p.annotation.name()) );  // required to get the correct unit type for the output
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
				return hand0.pair.annotation.name().compareTo(hand1.pair.annotation.name());

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

	public String getValueAsString(double simTime) {
		Class<? extends Unit> ut = this.getUnitType();
		String ret = this.getValueAsString(simTime, 1.0, "");
		if( ut != Unit.class && ut != DimensionlessUnit.class )
			ret += "  " + Unit.getSIUnit(ut);
		return ret;
	}

	public String getValueAsString(double simTime, String unitString, String format) {
		return this.getValueAsString(simTime, Input.parseUnits(unitString), format);
	}

	public String getValueAsString(double simTime, Unit unit, String format) {
		double factor = 1.0;
		Class<? extends Unit> ut = this.getUnitType();
		if( unit == null ) {
			if( ut != Unit.class && ut != DimensionlessUnit.class )
				return "Unit Mismatch";
		}
		else {
			factor = unit.getConversionFactorToSI();
			if( unit.getClass() != ut )
				return "Unit Mismatch";
		}
		return this.getValueAsString(simTime, factor, format);
	}

	public String getValueAsString(double simTime, double factor, String format) {
		try {
			Class<?> retType = this.getReturnType();
			if (retType == Double.class ||
			    retType == double.class) {
				double val = 0;
				if (retType == Double.class) {
					val = this.getValue(simTime, Double.class);
				} else {
					val = this.getValue(simTime, double.class);
				}
				if( format.isEmpty() )
					return String.format("%,.6g", val/factor);
				return String.format(format, val/factor);
			}

			Object o = pair.method.invoke(ent, simTime);
			if (o == null)
				return null;
			if( format.isEmpty() )
				return o.toString();
			return String.format(format, o.toString());

		} catch (InvocationTargetException ex) {
			assert false;
		} catch (IllegalAccessException ex) {
			assert false;
		}
		return null;
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
