/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.basicsim;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.jaamsim.events.ProcessTarget;

public class ReflectionTarget extends ProcessTarget {
	private final Entity target; // The entity whose method is to be executed
	private final Method method; // The method to be executed
	private final Object[] arguments; // The arguments passed to the method to be executed

	public ReflectionTarget(Entity ent, String methodName, Object... arguments) {
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
		return String.format("%s.%s", target.getName(), method.getName());
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
