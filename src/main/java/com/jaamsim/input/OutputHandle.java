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

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.StringVector;

/**
 * OutputHandle is a class that represents all the useful runtime information for an output,
 * specifically a reference to the runtime annotation and the method it points to
 * @author matt.chudleigh
 *
 */
public class OutputHandle {

	public Entity ent;
	public Output annotation;
	public Method method;
	public Class<? extends Unit> unitType;

	public OutputHandle(Entity e, Output a, Method m) {
		ent = e;
		annotation = a;
		method = m;

		assert (annotation != null);
		unitType =  annotation.unitType();
	}

	/**
	 * Return the OutputHandle obtained from an Entity and a chain of Output names.
	 * @param outputs = list containing athe Entity followed by the chain of Output names
	 * @param simTime = the simulation time at which the chain is to be evaluated
	 * @return = the OutputHandle corresponding to the last Output in the chain.
	 */
	public static OutputHandle getOutputHandle( StringVector outputs, double simTime) {

		if (outputs.size() < 2)
			return null;

		Entity e = Entity.getNamedEntity(outputs.get(0));

		// For any intermediate values (not the first or last), follow the entity-output chain
		for (int i = 1; i < outputs.size() - 1; ++i) {
			String outputName = outputs.get(i);
			if (e == null || !e.hasOutput(outputName, true))
				return null;
			e = e.getOutputHandle(outputName).getValue(simTime, Entity.class);
		}

		// Now get the last output, and take it's value from the current entity
		String name = outputs.get(outputs.size() - 1);

		if (e == null || !e.hasOutput(name, true))
			return null;

		return e.getOutputHandle(name);
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getValue(double simTime, Class<T> klass) {
		if (method == null) {
			return null;
		}

		T ret = null;
		try {
			if (!klass.isAssignableFrom(method.getReturnType()))
				return null;

			ret = (T)method.invoke(ent, simTime);
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

			Object o = method.invoke(ent, simTime);
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
		assert (method != null);
		return method.getReturnType();
	}

	public Class<?> getDeclaringClass() {
		assert (method != null);
		return method.getDeclaringClass();
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	public void setUnitType( Class<? extends Unit> ut ) {
		unitType = ut;
	}

	public String getDescription() {
		assert (annotation != null);
		return annotation.description();
	}
}
