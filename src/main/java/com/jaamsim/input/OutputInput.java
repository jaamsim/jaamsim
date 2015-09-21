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

public class OutputInput<T> extends Input<OutputChain> {

	private Class<T> klass;

	public OutputInput(Class<T> klass, String key, String cat, OutputChain def) {
		super(key, cat, def);
		this.klass = klass;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		value = Input.parseOutputChain(kw);
	}

	public OutputHandle getOutputHandle(double simTime) {
		return value.getOutputHandle(simTime);
	}

	public T getOutputValue(double simTime) {
		OutputHandle out = value.getOutputHandle(simTime);
		if( out == null )
			return null;
		return out.getValue(simTime, klass);
	}

	/**
	 * Returns the value of any numerical output converted to a double.
	 * @param simTime = present simulation time.
	 * @param def = the default value to return in case of null or a non-number.
	 * @return
	 */
	public double getOutputValueAsDouble(double simTime, double def) {
		OutputHandle out = value.getOutputHandle(simTime);
		if( out == null )
			return def;
		return out.getValueAsDouble(simTime, def);
	}

	@Override
	public void getValueTokens(java.util.ArrayList<String> toks) {
		if (value == null) return;

		toks.add(value.toString());
	}
}
