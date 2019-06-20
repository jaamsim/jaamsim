/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.Input;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.DimensionlessUnit;

public abstract class CompoundEntity extends LinkedComponent {

	@Keyword(description = "Determines whether to display the sub-model's components.",
	         exampleList = {"FALSE"})
	protected final BooleanInput showComponents;

	protected ArrayList<DisplayEntity> componentList;
	private SubModelStart smStart;

	{
		namedExpressionInput.setHidden(true); // FIXME CustomOutputList conflicts with the component outputs
		nextComponent.setRequired(false);

		showComponents = new BooleanInput("ShowComponents", FORMAT, true);
		this.addInput(showComponents);
	}

	public CompoundEntity() {
		componentList = new ArrayList<>();
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == showComponents) {
			showComponents(showComponents.getValue());
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Find the first component in the sub-model
		for (DisplayEntity comp : componentList) {
			if (comp instanceof SubModelStart) {
				smStart = (SubModelStart)comp;
				break;
			}
		}
	}

	public void setComponentList(ArrayList<DisplayEntity> list) {
		componentList = new ArrayList<>(list);
	}

	public ArrayList<DisplayEntity> getComponentList() {
		return componentList;
	}

	/**
	 * Displays or hides the sub-model's components.
	 * @param bool - if true, the components are displayed; if false, they are hidden.
	 */
	public void showComponents(boolean bool) {
		for (DisplayEntity comp : componentList) {
			InputAgent.applyBoolean(comp, "Show", showComponents.getValue());
		}
	}

	public boolean getShowComponents() {
		return showComponents.getValue();
	}

	@Override
	public void setName(String newName) {
		super.setName(newName);
		renameComponents(newName);
	}

	public void renameComponents(String newName) {
		for (DisplayEntity comp : componentList) {
			if (comp == null)
				continue;
			String name = getComponentName(newName, comp.getName());
			comp.setName(name);
		}
	}

	public void updateOutputs(ArrayList<DisplayEntity> oldCompList, ArrayList<DisplayEntity> newCompList) {

		// Do nothing if the components are unchanged
		if (newCompList.equals(oldCompList))
			return;

		// Delete the old outputs
		for (DisplayEntity comp : oldCompList) {
			if (comp == null || comp.getName() == null)
				continue;
			String outName = getComponentRootName(comp.getName());
			removeCustomOutput(outName);
		}

		// Build the new outputs
		for (int i = 0; i < newCompList.size(); i++) {
			DisplayEntity comp = newCompList.get(i);
			if (comp instanceof SubModelStart || comp instanceof SubModelEnd)
				continue;

			// Build a string for the output expression
			StringBuilder sb = new StringBuilder();
			sb.append("this.ComponentList(").append(i + 1).append(")");
			String expString = sb.toString();

			// Parse the expression string and add the output
			String outName = getComponentRootName(comp.getName());
			try {
				ExpEvaluator.EntityParseContext parseContext = ExpEvaluator.getParseContext(this, expString);
				Expression exp = ExpParser.parseExpression(parseContext, expString);
				addCustomOutput(outName, exp, DimensionlessUnit.class);
			}
			catch (ExpError e) {
				throw new ErrorException("Cannot create output '%s': " + e.getMessage(), outName);
			}
		}
	}

	/**
	 * Returns the component name without the sub-model's prefix.
	 * @param name - component name
	 * @return name with the prefix removed
	 */
	protected static String getComponentRootName(String name) {
		int percentIndex = name.indexOf('%', 1);
		return name.substring(percentIndex + 1);
	}

	/**
	 * Returns the name for a component entity based on the name of its sub-model.
	 * @param name - name of the sub-model.
	 * @param compName - present name of the component.
	 * @return new name for the component.
	 */
	protected static String getComponentName(String name, String compName) {
		StringBuilder sb = new StringBuilder();
		sb.append("%").append(name).append("%");
		sb.append(getComponentRootName(compName));
		return sb.toString();
	}

	protected String getComponentName(String name) {
		return getComponentName(this.getName(), name);
	}

	public boolean isComponentName(String name) {
		String compName = getComponentName(name);
		return name.equals(compName);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		smStart.addEntity(ent);
	}

	public void addReturnedEntity(DisplayEntity ent) {
		sendToNextComponent(ent);
	}

	@Output(name = "ComponentList",
	 description = "The objects contained in the sub-model.",
	    sequence = 0)
	public ArrayList<DisplayEntity> getComponentList(double simTime) {
		return componentList;
	}

}
