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
package com.jaamsim.basicsim;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.jaamsim.events.ProcessTarget;
import com.sandwell.JavaSimulation.Entity;

public class ReflectionTarget extends ProcessTarget {
	private final Entity target; // The entity whose method is to be executed
	private final Method method; // The method to be executed
	private final Object[] arguments; // The arguments passed to the method to be executed

	public ReflectionTarget(Entity ent, String methodName, Object[] arguments) {
		target = ent;
		method = findEntityMethod(target.getClass(), methodName, arguments);
		this.arguments = arguments;
	}

	@Override
	public void process() {
		try {
			method.invoke(target, arguments);
		}
		// Normal exceptions thrown by the method called by invoke are wrapped
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException)cause;
			else
				throw new ErrorException(cause);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new ErrorException(e);
		}
	}

	@Override
	public String getDescription() {
		return String.format("%s.%s", target.getInputName(), method.getName());
	}

	// Look up the method with the given name for the given entity and argument list.
	private Method findEntityMethod(Class<?> targetClass, String methodName, Object... arguments) {
		Class<?>[] argClasses = new Class<?>[arguments.length];

		// Fill in the class of each argument, if there are any
		for (int i = 0; i < arguments.length; i++) {
			// The argument itself is null, no class information available
			if (arguments[i] == null) {
				argClasses[i] = null;
				continue;
			}

			argClasses[i] = arguments[i].getClass();

			// We wrap primitive doubles as Double, put back the primitive type
			if (argClasses[i] == Double.class) {
				argClasses[i] = Double.TYPE;
			}

			// We wrap primitive integers as Integer, put back the primitive type
			if (argClasses[i] == Integer.class) {
				argClasses[i] = Integer.TYPE;
			}
		}

		// Attempt to lookup the method using exact type information
		try {
			return targetClass.getMethod(methodName, argClasses);
		}
		catch (SecurityException e) {
			throw new ErrorException("Security Exception when finding method: %s", methodName);
		}
		catch (NullPointerException e) {
			throw new ErrorException("Name passed to startProcess was NULL");
		}
		catch (NoSuchMethodException e) {
			// Get a list of all our methods
			Method[] methods = targetClass.getMethods();

			// Loop over all methods looking for a unique method name
			int matchIndexHolder = -1;
			int numMatches = 0;
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(methodName)) {
					numMatches++;
					matchIndexHolder = i;
				}
			}

			// If there was only one method found, use it
			if (numMatches == 1)
				return methods[matchIndexHolder];
			else
			throw new ErrorException("Method: %s does not exist, could not invoke.", methodName);
		}
	}
}
