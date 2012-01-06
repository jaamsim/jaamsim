/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import java.lang.reflect.Method;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;

public class PropertyLabel extends TextLabel  {
	private Method targetMethod;  		// the method which used to calculate the y value
	private Object[] targetInputParameters;
	private final Object[] timeMethodParameter;

	private final EntityInput<Entity> targetEntity;
	private final StringInput targetMethodName;
	private final StringListInput targetInputParameterNames;
	private final BooleanInput timeMethod;
	private final IntegerInput precision;
	private final DoubleInput multiplier;
	private final StringInput units;
	private final StringInput prefix;

	private String doubleFmt;
	private String fmt;

	{
		targetEntity = new EntityInput<Entity>(Entity.class, "TargetEntity", "Key Inputs", null);
		this.addInput(targetEntity, true);

		targetMethodName = new StringInput("TargetMethod", "Key Inputs", "");
		this.addInput(targetMethodName, true);

		targetInputParameterNames = new StringListInput("TargetInputParameters", "Key Inputs", new StringVector(0));
		this.addInput(targetInputParameterNames, true);

		timeMethod = new BooleanInput("TimeMethod", "Key Inputs", false);
		this.addInput(timeMethod, true);

		precision = new IntegerInput("Precision", "Optional", 0);
		precision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(precision, true);

		multiplier = new DoubleInput("Multiplier", "Optional", 1.0d);
		this.addInput(multiplier, true);

		units = new StringInput("Units", "Optional", "");
		this.addInput(units, true);

		prefix = new StringInput("Prefix", "Optional", "");
		this.addInput(prefix, true);
	}

	public PropertyLabel() {
		timeMethodParameter = new Object[1];
	}

	public void earlyInit() {
		super.earlyInit();

		doubleFmt = String.format("%s%%,.%df %s", prefix.getValue(), precision.getValue(), units.getValue());
		fmt  = String.format("%s%%s", prefix.getValue());

		// Populate target method from its name and parameters class type
		Class<?>[] parameterTypes = new Class[targetInputParameterNames.getValue().size()];
		targetInputParameters = new Object[targetInputParameterNames.getValue().size()];

		// Target method accepts input parameter
		for (int i = 0; i < targetInputParameterNames.getValue().size(); i++) {
			String str = targetInputParameterNames.getValue().get(i);
			Entity ent = simulation.getEntityWithName( str );
			if( ent != null ) {
				targetInputParameters[ i ] = ent;
			}
			else {
				// Allow String input parameters
				// TODO - Allow Double and Integer input parameters
				targetInputParameters[ i ] = str;
			}
			parameterTypes[ i ] = targetInputParameters[ i ].getClass();
		}

		// Target method accepts time as input parameter
		if( timeMethod.getValue() ) {
			parameterTypes = new Class[] { double.class };
		}
		try {
			targetMethod = targetEntity.getValue().getClass().getMethod( targetMethodName.getValue(), parameterTypes  );
		} catch (SecurityException e) {
			throw new SecurityException( "Method:" + targetEntity.getValue() + "." + targetMethodName.getValue() + " is not accessible" );
		} catch ( NoSuchMethodException e) {
			throw new InputErrorException( "Method:" + targetEntity.getValue() + "." + targetMethodName.getValue() + " does not exist" );
		}
		this.initializeGraphics();
		this.enterRegion();
	}

	String getRenderText(double time) {
		Object[] params;
		// Time is passing to the method as an argument
		if (timeMethod.getValue()) {
			timeMethodParameter[0] = time;
			params = timeMethodParameter;
		}
		else {
			params = targetInputParameters;
		}

		try {
			// run target method and return its value
			Object value = targetMethod.invoke(targetEntity.getValue(), params);
			if (value instanceof Double)
				return String.format(doubleFmt, ((Double)value).doubleValue() * multiplier.getValue());
			else
				return String.format(fmt, value);

		}
		catch (Throwable e) {
			return super.getRenderText(time);
		}
	}
}
