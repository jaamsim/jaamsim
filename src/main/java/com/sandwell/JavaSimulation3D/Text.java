/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;

/**
 * The "Text" object displays written text within the 3D model universe.  Both fixed and variable text can be displayed.
 * @author Harry King
 *
 */
public class Text extends DisplayEntity {

	@Keyword(description = "The fixed and variable text to be displayed.  If spaces are included, enclose the text in single quotes.  " +
			"If variable text is to be displayed using the OutputName keyword, include the appropriate Java format in the text, " +
			"e.g. %s, %.6f, %.6g",
	         example = "Text-1 Format { 'Present speed = %.3f m/s' }")
	protected final StringInput formatText;

	@Keyword(description = "The output value chain that returns the variable text to be displayed. " +
			"If more than one output value is given, all outputs but the last should point to an entity output to query" +
			" for the next output. The example returns the name of the product in a tank",
	         example = "Text-1 OutputName { Tank1 Product Name }")
	protected final StringListInput outputName;

	@Keyword(description = "The unit inwhich to express the output value",
	         example = "Text-1 Unit { m/s }")
	protected final EntityInput<Unit> unit;

	@Keyword(description = "The height of the font as displayed in the view window.",
	         example = "Text-1 TextHeight { 15 m }")
	protected final ValueInput textHeight;

	protected String renderText = "";
	private String invalidOutputName = "Invalid entry for keyword OutputName";

	{
		formatText = new StringInput("Format", "Key Inputs", "abc");
		this.addInput(formatText, true);

		outputName = new StringListInput("OutputName", "Key Inputs", null);
		this.addInput(outputName, true);

		unit = new EntityInput<Unit>( Unit.class, "Unit", "Key Inputs", null);
		this.addInput(unit, true);

		textHeight = new ValueInput("TextHeight", "Key Inputs", 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		this.addInput(textHeight, true);

		InputAgent.processEntity_Keyword_Value(this, "Size", "1.0 1.0 0.0 m");
	}

	public Text() {
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == textHeight) {
			setGraphicsDataDirty();
		}
	}

	public String getRenderText(double simTime) {
		String ret = "";
		StringVector outputs = outputName.getValue();
		if( outputs == null || outputs.isEmpty() )
			return formatText.getValue();

		if (outputs.size() < 2)
			return invalidOutputName;

		Entity ent = Entity.getNamedEntity(outputs.get(0));

		// For any intermediate values (not the first or last), follow the entity-output chain
		for (int i = 1; i < outputs.size() - 1; ++i) {
			String outputName = outputs.get(i);
			if (ent == null || !ent.hasOutput(outputName, true))
				return invalidOutputName;
			ent = ent.getOutputHandle(outputName).getValue(simTime, Entity.class);
		}

		// Now get the last output, and take it's value from the current entity
		String name = outputs.get(outputs.size() - 1);

		if (ent == null || !ent.hasOutput(name, true))
			return invalidOutputName;

		OutputHandle out = ent.getOutputHandle(name);
		if( out == null )
			return invalidOutputName;
		ret = out.getValueAsString(simTime, unit.getValue(), formatText.getValue());
		if( ret == null )
			return "Invalid entry for keyword Format";
		return ret;
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// This text is cached because reflection is used to get it, so who knows how long it will take
		String newRenderText = getRenderText(simTime);
		if (newRenderText.equals(renderText)) {
			// Nothing important has changed
			return;
		}

		// The text has been updated
		setGraphicsDataDirty();
		renderText = newRenderText;

	}

	public String getCachedText() {
		return renderText;
	}

	public double getTextHeight() {
		return textHeight.getValue().doubleValue();
	}

}
