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
import com.sandwell.JavaSimulation.ObjectType;

/**
 * OutputListInput is an object for parsing inputs consisting of a list of OutputInputs using the syntax:\n
 * Entity keyword { { Entity1 Output1 } { Entity2 Output2 } ... }/n
 * where Output1 is an Output of Entity1, etc.
 * @author Harry King
 *
 * @param <T> = Class returned by each of the OutputInputs in the list
 */
public class OutputListInput<T> extends ListInput<ArrayList<OutputHandle>> {

	private Class<T> klass;  // class returned by each OutputInput in the list

	public OutputListInput(Class<T> klass, String key, String cat, ArrayList<OutputHandle> def) {
		super(key, cat, def);
		this.klass = klass;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<OutputHandle> temp = new ArrayList<OutputHandle>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2);
			try {
				Entity ent = Input.parseEntity(subArg.getArg(0), Entity.class);
				if (ent instanceof ObjectType)
					throw new InputErrorException("%s is the name of a class, not an instance", ent.getName());
				String outputName = subArg.getArg(1);
				if (!ent.hasOutput(outputName)) {
					throw new InputErrorException("Output named %s not found for Entity %s", outputName, ent.getName());
				}
				OutputHandle out = ent.getOutputHandle(outputName);
				Class<?> retClass = out.getReturnType();
					if( (klass == Double.class && retClass != Double.class && retClass != double.class) ||
						(klass != Double.class && klass != Object.class && !klass.isAssignableFrom(retClass)) )
					throw new InputErrorException("OutputInput class mismatch. Expected: %s, got: %s", klass.toString(), retClass.toString());
				temp.add(out);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public T getOutputValue(int i, double simTime) {
		return value.get(i).getValue(simTime, klass);
	}

	@Override
	public String getValueString() {
		if( value == null)
			return "";

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < value.size(); i++) {
			if (i > 0) tmp.append(SEPARATOR);
			OutputHandle out = value.get(i);
			tmp.append("{ ");
			tmp.append(out.ent.getInputName());
			tmp.append(SEPARATOR);
			tmp.append(out.getName());
			tmp.append(" }");
		}
		return tmp.toString();
	}
}
