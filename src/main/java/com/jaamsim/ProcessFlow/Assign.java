/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.AssignmentListInput;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.Keyword;

/**
 * Assigns values to Attributes.
 * @author Harry King
 *
 */
public class Assign extends LinkedComponent {

	@Keyword(description = "A list of attribute assignments that are triggered when an entity is "
	                     + "received."
	                     + "\n\n"
	                     + "The attributes for various entities can be used in an assignment "
	                     + "expression:\n"
	                     + "- this entity -- this.AttributeName\n"
	                     + "- entity received -- this.obj.AttributeName\n"
	                     + "- another entity -- [EntityName].AttributeName",
	         exampleList = {"{ 'this.A = 1' } { 'this.obj.B = 1' } { '[Ent1].C = 1' }",
	                        "{ 'this.D = 1[s] + 0.5*this.SimTime' }"})
	private final AssignmentListInput assignmentList;

	{
		assignmentList = new AssignmentListInput("AttributeAssignmentList", KEY_INPUTS, new ArrayList<ExpParser.Assignment>());
		assignmentList.setRequired(true);
		this.addInput(assignmentList);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Evaluate the assignment expressions
		assignmentList.executeAssignments(this, getSimTime());

		// Pass the entity to the next component
		this.sendToNextComponent(ent);
	}

}
