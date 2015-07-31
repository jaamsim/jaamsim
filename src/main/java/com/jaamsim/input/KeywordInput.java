/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;

public class KeywordInput extends Input<String> {
	private DisplayEntity targetEntity;
	private Input<?> targetInput;

	public KeywordInput(String key, String cat, String def) {
		super(key, cat, def);
		targetEntity = null;
		targetInput = null;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		KeywordInput inp = (KeywordInput) in;
		targetEntity = inp.targetEntity;
		targetInput = inp.targetInput;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 2);
		try {
			DisplayEntity ent = Input.parseEntity(kw.getArg(0), DisplayEntity.class);

			String key = kw.getArg(1);
			Input<?> in = ent.getInput(key);
			if (in == null)
				throw new InputErrorException("'%s' is not a valid keyword for entity '%s'", key, ent);

			value = key;
			targetEntity = ent;
			targetInput = in;
		}
		catch (InputErrorException e) {
			throw new InputErrorException(e.getMessage());
		}
	}

	public Input<?> getTargetInput() {
		return targetInput;
	}

	public DisplayEntity getTargetEntity() {
		return targetEntity;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;
		toks.add(targetEntity.getName());
		toks.add(value);
	}

}
