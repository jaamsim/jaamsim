/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023 JaamSim Software Inc.
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

import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;

public class OutputMethod extends Entity {

@Keyword(description = "The target Entity we are collecting output from")
private final EntityInput<Entity> target;

@Keyword(description = "The name of the method to call on the target",
         exampleList = {"getAmountLoaded"})
private final StringInput method;

@Keyword(description = "A list of arguments to pass to the target method, these will be "
                     + "interpreted as Entities, then Integers and finally as literal Strings",
         exampleList = {"Entity1 5 'A String'"})
private final StringListInput arguments;

{
	target = new EntityInput<>(Entity.class, "Target", "Outputs", null);
	this.addInput(target);

	method = new StringInput("Method", "Outputs", null);
	this.addInput(method);

	arguments = new StringListInput("Arguments", "Outputs", null);
	this.addInput(arguments);
}

public OutputMethod() {}

public String getReportLine() {
	StringBuilder str = new StringBuilder();
	str.append(target.getValue().getName());
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
		Entity e = Input.tryParseEntity(this.getJaamSimModel(), arg, Entity.class);
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
