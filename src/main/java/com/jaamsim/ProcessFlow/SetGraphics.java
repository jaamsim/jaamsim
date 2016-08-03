/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
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
	private final SampleInput choice;

	{
		targetEntity = new EntityInput<>( DisplayEntity.class, "TargetEntity", "Key Inputs", null);
		targetEntity.setDefaultText("This Entity");
		this.addInput( targetEntity);

		graphicsList = new EntityListInput<>( DisplayEntity.class, "GraphicsList", "Key Inputs", null);
		graphicsList.setRequired(true);
		ArrayList<Class<? extends Entity>> list = new ArrayList<>();
		list.add(TextBasics.class);
		list.add(OverlayEntity.class);
		graphicsList.setInvalidClasses(list);
		this.addInput( graphicsList);

		choice = new SampleInput("Choice", "Key Inputs", new SampleConstant(DimensionlessUnit.class, 1.0));
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
