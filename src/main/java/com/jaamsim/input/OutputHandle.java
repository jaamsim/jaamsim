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
		String ret = null;
		try {
			Object o = method.invoke(ent, simTime);
			if (o == null)
				return null;

			ret = o.toString();
			Class<? extends Unit> ut = annotation.unitType();
			if( ut != Unit.class && ut != DimensionlessUnit.class )
				ret += "  " + Unit.getSIUnit(ut);

			return ret;

		} catch (InvocationTargetException ex) {
			assert false;
		} catch (IllegalAccessException ex) {
			assert false;
		}
		return ret;
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
