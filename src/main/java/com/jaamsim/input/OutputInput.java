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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class OutputInput<T> extends Input<String> {

	private Class<T> klass;
	private Entity ent;  // The Entity against which to apply the first Output name
	private String outputName;  // The first Output name in the chain
	private OutputHandle out;  // The OutputHandle for the first Output in the chain
	private StringVector outputNameList;  // The names of the second, third, etc. Outputs in the chain.

	public OutputInput(Class<T> klass, String key, String cat, String def) {
		super(key, cat, def);
		this.klass = klass;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {

		if( input.isEmpty() ) {
			value = null;
			ent = null;
			outputName = "";
			out = null;
			outputNameList.clear();
			return;
		}

		Input.assertCountRange(input, 2, Integer.MAX_VALUE);
		Entity tmp = Input.parseEntity(input.get(0), Entity.class);
		String outName = input.get(1);
		if (!tmp.hasOutput(outName)) {
			throw new InputErrorException("Output named %s not found for Entity %s", outName, tmp.getName());
		}

		ent = tmp;
		outputName = outName;
		out = ent.getOutputHandle(outputName);

		outputNameList = new StringVector();
		if( input.size() > 2 )
			outputNameList = input.subString(2, input.size()-1);

		Class<?> retClass = out.getReturnType();
		if( input.size() == 2 ) {
			if ( klass != Object.class && !klass.isAssignableFrom(retClass) )
				throw new InputErrorException("OutputInput class mismatch. Expected: %s, got: %s", klass.toString(), retClass.toString());
		}
		else {
			if (!(Entity.class).isAssignableFrom(retClass))
				throw new InputErrorException("OutputInput class mismatch. The first output in the output chain must return an Entity");
		}

		value = String.format("%s.%s", ent.getInputName(), outputName);
		if( input.size() > 2 ) {
			for( String name: outputNameList ) {
				value += "." + name;
			}
		}
	}

	public OutputHandle getOutputHandle(double simTime) {
		OutputHandle o = out;
		for( String name : outputNameList ) {
			Entity e = o.getValue(simTime, Entity.class);
			if( e == null || !e.hasOutput(name) )
				return null;
			o = e.getOutputHandle(name);
		}
		return o;
	}

	public T getOutputValue(double simTime) {
		OutputHandle o = this.getOutputHandle(simTime);
		if( o == null )
			return null;
		return o.getValue(simTime, klass);
	}

	/**
	 * Returns the value of any numerical output converted to a double.
	 * @param simTime = present simulation time.
	 * @param def = the default value to return in case of null or a non-number.
	 * @return
	 */
	public double getOutputValueAsDouble(double simTime, double def) {
		OutputHandle o = this.getOutputHandle(simTime);
		if( o == null )
			return def;
		return o.getValueAsDouble(simTime, def);
	}

	@Override
	public String getValueString() {
		if (ent == null)
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(ent.getInputName());
		tmp.append(SEPARATOR);
		tmp.append(outputName);
		for( String name : outputNameList ) {
			tmp.append(SEPARATOR);
			tmp.append(name);
		}
		return tmp.toString();
	}

}
