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
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;

public class SetGraphics extends LinkedComponent {

	@Keyword(description = "The entity whose graphics are to be changed. Defaults to the entity that was received.",
	         exampleList = {"Server1"})
	private final EntityInput<DisplayEntity> targetEntity;

	@Keyword(description = "List of entities whose graphics can chosen for assignment to the target entity.",
	         exampleList = {"DisplayEntity1 DisplayEntity2"})
	private final EntityListInput<DisplayEntity> graphicsList;

	@Keyword(description = "A number that determines the choice of entities from the GraphicsList:\n" +
			"     1 = first entity's graphics, 2 = second entity's graphics, etc.\n" +
			"A constant value, a distribution to be sampled, or a time series can be entered.",
	         exampleList = {"2", "DiscreteDistribution1", "'1 + [TimeSeries1].PresentValue'"})
	private final SampleExpInput choice;

	{
		targetEntity = new EntityInput<>( DisplayEntity.class, "TargetEntity", "Key Inputs", null);
		targetEntity.setDefaultText("This Entity");
		this.addInput( targetEntity);

		graphicsList = new EntityListInput<>( DisplayEntity.class, "GraphicsList", "Key Inputs", null);
		graphicsList.setRequired(true);
		this.addInput( graphicsList);

		choice = new SampleExpInput("Choice", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1.0));
		choice.setUnitType(DimensionlessUnit.class);
		choice.setEntity(this);
		choice.setValidRange(1, Double.POSITIVE_INFINITY);
		this.addInput(choice);
	}

	public SetGraphics() {}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Identify the entity whose graphics are to be changed
		DisplayEntity target = targetEntity.getValue();
		if (target == null)
			target = ent;

		// Choose the new graphics for this entity
		int i = (int) choice.getValue().getNextSample(this.getSimTime());
		if (i<1 || i>graphicsList.getValue().size())
			error("Chosen index i=%s is out of range for GraphicList: %s.", i, graphicsList.getValue());
		DisplayEntity chosen = graphicsList.getValue().get(i-1);

		target.setDisplayModelList(chosen.getDisplayModelList());
		target.setSize(chosen.getSize());
		target.setOrientation(chosen.getOrientation());
		target.setAlignment(chosen.getAlignment());

		// Send the entity to the next component in the chain
		this.sendToNextComponent(ent);
	}

}
