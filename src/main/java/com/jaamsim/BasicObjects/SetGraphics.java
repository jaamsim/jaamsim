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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class SetGraphics extends LinkedComponent {

	@Keyword(description = "List of entities whose graphics can chosen for assignment to the received entity.",
	         example = "SetGraphics1 GraphicsList{ DisplayEntity1 DisplayEntity2 }")
	private final EntityListInput<DisplayEntity> graphicsList;

	@Keyword(description = "A number that determines the choice of entities from the GraphicsList:\n" +
			"     1 = first entity's graphics, 2 = second entity's graphics, etc.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         example = "SetGraphics1 Choice { 2 }")
	private final SampleExpInput choice;

	{
		graphicsList = new EntityListInput<DisplayEntity>( DisplayEntity.class, "GraphicsList", "Key Inputs", null);
		this.addInput( graphicsList);

		choice = new SampleExpInput("Choice", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1.0));
		choice.setUnitType(DimensionlessUnit.class);
		choice.setEntity(this);
		this.addInput(choice);
	}

	public SetGraphics() {}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the GraphicList has been specified
		if (graphicsList.getValue() == null || graphicsList.getValue().isEmpty()) {
			throw new InputErrorException("The keyword GraphicsList must be set.");
		}
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Choose the new graphics for this entity
		int i = (int) choice.getValue().getNextSample(this.getSimTime());
		if (i<1 || i>graphicsList.getValue().size())
			throw new ErrorException("Chosen index i=%s is out of range for GraphicList: %s.", i, graphicsList.getValue());
		DisplayEntity chosen = graphicsList.getValue().get(i-1);

		// Set the graphics for the incoming entity to those for the chosen entity
		String str = chosen.getInput("DisplayModel").getValueString();
		InputAgent.processEntity_Keyword_Value(ent, "DisplayModel", str);

		ent.setSize(chosen.getSize());
		ent.setOrientation(chosen.getOrientation());
		ent.setAlignment(chosen.getAlignment());

		// Send the entity to the next component in the chain
		this.sendToNextComponent(ent);
	}

}
