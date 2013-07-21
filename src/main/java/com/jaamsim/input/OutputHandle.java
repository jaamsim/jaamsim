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

/**
 * OutputHandle is a class that represents all the useful runtime information for an output,
 * specifically a reference to the runtime annotation and the method it points to
 * @author matt.chudleigh
 *
 */
public class OutputHandle {

	public Output annotation;
	public Method method;

	public OutputHandle(Output a, Method m) {
		annotation = a;
		method = m;
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getValue(Entity ent, double simTime, Class<T> klass) {
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

	public String getValueAsString(Entity ent, double simTime) {
		Class<? extends Unit> ut = this.getUnitType();
		String ret = this.getValueAsString(ent, simTime, 1.0, "");
		if( ut != Unit.class && ut != DimensionlessUnit.class )
			ret += "  " + Unit.getSIUnit(ut);
		return ret;
	}

	public String getValueAsString(Entity ent, double simTime, String unitString, String format) {
		return this.getValueAsString(ent, simTime, Input.parseUnits(unitString), format);
	}

	public String getValueAsString(Entity ent, double simTime, Unit unit, String format) {
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
		return this.getValueAsString(ent, simTime, factor, format);
	}

	public String getValueAsString(Entity ent, double simTime, double factor, String format) {
		try {
			Class<?> retType = this.getReturnType();
			if (retType == Double.class ||
			    retType == double.class) {
				double val = 0;
				if (retType == Double.class) {
					val = this.getValue(ent, simTime, Double.class);
				} else {
					val = this.getValue(ent, simTime, double.class);
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
		assert (annotation != null);
		return annotation.unitType();
	}

	public String getDescription() {
		assert (annotation != null);
		return annotation.description();
	}
}
