/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

public class AliasListInput extends ListInput<StringVector> {
	protected Entity currentEntity; // the entity for which the alias list applies

	public AliasListInput(String key, String cat, StringVector def, Entity ent ) {
		super(key, cat, def);
		currentEntity = ent;
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, minCount, maxCount);

		// Set each string to be an alias of the entity
		for( String str : input ) {
			currentEntity.setAlias( str );
		}

		value = input;
		this.updateEditingFlags();
	}
}
