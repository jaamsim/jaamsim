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

import java.util.ArrayList;

import com.sandwell.JavaSimulation.Entity;

public class OutputInput<T> extends Input<String> {

	private Class<T> klass;
	private Entity ent;  // The Entity against which to apply the first Output name
	private String outputName;  // The first Output name in the chain
	private OutputHandle out;  // The OutputHandle for the first Output in the chain
	private ArrayList<String> outputNameList;  // The names of the second, third, etc. Outputs in the chain.

	public OutputInput(Class<T> klass, String key, String cat, String def) {
		super(key, cat, def);
		this.klass = klass;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		if (kw.numArgs() == 0) {
			value = null;
			ent = null;
			outputName = "";
			out = null;
			outputNameList.clear();
			return;
		}

		Input.assertCountRange(kw, 2, Integer.MAX_VALUE);
		Entity tmp = Input.parseEntity(kw.getArg(0), Entity.class);
		String outName = kw.getArg(1);
		if (!tmp.hasOutput(outName)) {
			throw new InputErrorException("Output named %s not found for Entity %s", outName, tmp.getName());
		}

		ent = tmp;
		outputName = outName;
		out = ent.getOutputHandle(outputName);

		outputNameList = new ArrayList<String>(kw.numArgs() - 2);
		// grab any input strings after the first two, if there are any
		for (int i = 2; i < kw.numArgs(); i++)
			outputNameList.add(kw.getArg(i));

		Class<?> retClass = out.getReturnType();
		if( kw.numArgs() == 2 ) {
			if ( klass != Object.class && !klass.isAssignableFrom(retClass) )
				throw new InputErrorException("OutputInput class mismatch. Expected: %s, got: %s", klass.toString(), retClass.toString());
		}
		else {
			if (!(Entity.class).isAssignableFrom(retClass))
				throw new InputErrorException("OutputInput class mismatch. The first output in the output chain must return an Entity");
		}

		value = String.format("%s.%s", ent.getInputName(), outputName);
		if( kw.numArgs() > 2 ) {
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
