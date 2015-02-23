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
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * SampleListInput is an object for parsing inputs consisting of a list of
 * SampleProviders using the syntax:
 * <p>
 * Entity keyword { SampleProvider1  SampleProvider2 ... }
 * <p>
 * where SampleProvider1, etc. are Entities that implement the SampleProvider interface.
 * @author Harry King
 */
public class SampleListInput extends ListInput<ArrayList<SampleProvider>> {

	private Class<? extends Unit> unitType = DimensionlessUnit.class;

	public SampleListInput(String key, String cat, ArrayList<SampleProvider> def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u != unitType)
			this.reset();
		unitType = u;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {

		ArrayList<SampleProvider> temp = new ArrayList<>(kw.numArgs());

		// Try to parse as constant values
		try {
			DoubleVector tmp = Input.parseDoubles(kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			Input.assertCount(tmp, kw.numArgs()-1);
			for( int i = 0; i < tmp.size(); i++ ) {
				SampleProvider s = new SampleConstant(unitType, tmp.get(i));
				temp.add( s );
			}
			value = temp;
			return;
		}
		catch (InputErrorException e) {}

		for (int i = 0; i < kw.numArgs(); i++) {

			Entity ent = Input.parseEntity(kw.getArg(i), Entity.class);
			SampleProvider s = Input.castImplements(ent, SampleProvider.class);
			if( s.getUnitType() != UserSpecifiedUnit.class )
				Input.assertUnitsMatch(unitType, s.getUnitType());

			temp.add(s);
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

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (samp.getUnitType() == unitType)
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++) {
			toks.add(value.get(i).toString());
		}
	}
}
