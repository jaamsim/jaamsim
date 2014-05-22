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

import java.util.ArrayList;

import com.jaamsim.input.AssignmentListInput;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * Assigns values to Attributes.
 * @author Harry King
 *
 */
public class Assign extends LinkedComponent {

	@Keyword(description = "A list of attribute assignments that are triggered when an entity is received.\n\n" +
			"The attributes for various entities can be used in an assignment expression:\n" +
			"- this entity -- this.AttributeName\n" +
			"- the entity received -- this.obj.AttributeName\n" +
			"- another entity -- [EntityName].AttributeName",
	         example = "Assign1 AttributeAssignmentList { {'this.A = this.A + 1'} {'this.obj.B = 1'} {'[Ent1].C = 0'} }")
	private final AssignmentListInput assignmentList;

	{
		assignmentList = new AssignmentListInput("AttributeAssignmentList", "Key Inputs", new ArrayList<ExpParser.Assignment>());
		this.addInput(assignmentList);
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Evaluate the assignment expressions
		for (ExpParser.Assignment ass : assignmentList.getValue()) {
			try {
				ExpEvaluator.runAssignment(ass, getSimTime(), this, ent);
			} catch (ExpEvaluator.Error err) {
				throw new RuntimeException(err.getMessage());
			}
		}

		// Pass the entity to the next component
		this.sendToNextComponent(ent);
	}

}
