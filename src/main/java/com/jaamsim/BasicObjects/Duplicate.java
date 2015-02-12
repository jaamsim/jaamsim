/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;

public class Duplicate extends LinkedComponent {

	@Keyword(description = "The list of components that will receive the duplicated entities. " +
			"One duplicated entity will be sent to each entry in the list.",
	         example = "Branch1 TargetComponentList { Object1 Object2 }")
	protected final EntityListInput<LinkedComponent> targetComponentList;

	{
		targetComponentList = new EntityListInput<>( LinkedComponent.class, "TargetComponentList", "Key Inputs", null);
		this.addInput( targetComponentList);
	}

	public Duplicate() {}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Make the duplicates and send them to the targets
		int n = 1;
		for (LinkedComponent target : targetComponentList.getValue()) {

			// Create the duplicated entity
			StringBuilder sb = new StringBuilder();
			sb.append(ent.getName()).append("_Dup").append(n);
			DisplayEntity dup = (DisplayEntity) Entity.fastCopy(ent, sb.toString());
			dup.earlyInit();

			// Send the duplicate to the target component
			target.addEntity(dup);
			n++;
		}

		// Send the received entity to the next component
		this.sendToNextComponent(ent);
	}

}
