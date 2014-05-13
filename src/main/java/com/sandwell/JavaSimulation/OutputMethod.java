/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;

public class OutputMethod extends Entity {

@Keyword(description = "The target Entity we are collecting output from",
         example = "Output1 Target { Entity1 }")
private final EntityInput<Entity> target;

@Keyword(description = "The name of the method to call on the target",
         example = "Output1 Method { getAmountLoaded }")
private final StringInput method;

@Keyword(description = "A list of arguments to pass to the target method, these will be interpreted" +
                "Entities, then Integers and finally as literal Strings",
         example = "Output1 Arguments { Entity1 5 'A String' }")
private final StringListInput arguments;

{
	target = new EntityInput<Entity>(Entity.class, "Target", "Outputs", null);
	this.addInput(target);

	method = new StringInput("Method", "Outputs", null);
	this.addInput(method);

	arguments = new StringListInput("Arguments", "Outputs", null);
	this.addInput(arguments);
}

public OutputMethod() {}

public String getReportLine() {
	StringBuilder str = new StringBuilder();
	str.append(target.getValue().getInputName());
	str.append(" ");
	str.append(method.getValue());
	if (arguments.getValue() != null) {
		for (String arg : arguments.getValue()) {
			str.append(" ");
			str.append(arg);
		}
	}

	this.appendOutput(str);
	return str.toString();
}

private void appendOutput(StringBuilder bld) {

	Entity ent = target.getValue();
	if (ent == null) {
		bld.append("\t#ERROR");
		return;
	}

	int numParams = 0;
	if (arguments.getValue()!= null)
		numParams = arguments.getValue().size();

	Object[] params = new Object[numParams];
	Class<?>[] paramClass = new Class<?>[numParams];

	for (int i = 0; i < numParams; i++) {
		String arg = arguments.getValue().get(i);

		// Treat as an Entity if possible
		Entity e = Input.tryParseEntity(arg, Entity.class);
		if (e != null) {
			params[i] = e;
			paramClass[i] = e.getClass();
			continue;
		}

		// Otherwise, check if it is an Integer
		if (Input.isInteger(arg)) {
			params[i] = Integer.parseInt(arg);
			paramClass[i] = Integer.class;
			continue;
		}

		// Last resort, pass through a String directly
		params[i] = arg;
		paramClass[i] = String.class;
	}

	Method meth = null;
	try {
		meth = ent.getClass().getMethod(method.getValue(), paramClass);
	}
	catch (NoSuchMethodException e) {}
	catch (SecurityException e) {}

	if (meth == null) {
		bld.append("\t#ERROR");
		return;
	}

	try {
		Object ret = meth.invoke(ent, params);

		if (ret instanceof Double) {
			double d = ((Double)ret).doubleValue();

			if (Double.isNaN(d)) {
				bld.append("\t#ERROR");
				return;
			}
			else {
				bld.append("\t").append(d);
				return;
			}
		}

		if (ret instanceof Integer) {
			int i = ((Integer)ret).intValue();
			bld.append("\t").append(i);
			return;
		}

		bld.append("\t#ERROR");
	}
	catch (IllegalArgumentException e) {}
	catch (IllegalAccessException e) {}
	catch (InvocationTargetException e) {}
	catch (ExceptionInInitializerError e) {}
}
}
