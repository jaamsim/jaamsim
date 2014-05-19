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

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.OutputInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.StringInput;

/**
 * The "Text" object displays written text within the 3D model universe.  Both fixed and variable text can be displayed.
 * @author Harry King
 *
 */
public class Text extends DisplayEntity {

	@Keyword(description = "The fixed and variable text to be displayed.  If spaces are included, enclose the text in single quotes.  " +
			"If variable text is to be displayed using the OutputName keyword, include the appropriate Java format in the text, " +
			"e.g. %s, %.6f, %.6g",
	         example = "Text1 Format { 'Present speed = %.3f m/s' }")
	protected final StringInput formatText;

	@Keyword(description = "The output value chain that returns the variable text to be displayed. " +
			"If more than one output value is given, all outputs but the last should point to an entity output to query" +
			" for the next output. The example returns the name of the product in a tank",
	         example = "Text1 OutputName { Tank1 Product Name }")
	protected final OutputInput<Object> outputName;

	@Keyword(description = "The unit in which to express the output value",
	         example = "Text1 Unit { m/s }")
	protected final EntityInput<Unit> unit;

	@Keyword(description = "The height of the font as displayed in the view window.",
	         example = "Text1 TextHeight { 15 m }")
	protected final ValueInput textHeight;

	@Keyword(description = "The text to display if there is any failure while formatting" +
	                       "the dynamic text, or while reading the output's value.",
	         example = "Text1 FailText { '' }")
	private final StringInput failText;

	protected String renderText = "";

	{
		formatText = new StringInput("Format", "Key Inputs", "abc");
		this.addInput(formatText);

		outputName = new OutputInput<Object>(Object.class, "OutputName", "Key Inputs", null);
		this.addInput(outputName);

		unit = new EntityInput<Unit>( Unit.class, "Unit", "Key Inputs", null);
		this.addInput(unit);

		textHeight = new ValueInput("TextHeight", "Key Inputs", 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		this.addInput(textHeight);

		failText = new StringInput("FailText", "Key Inputs", "");
		this.addInput(failText);
	}

	public Text() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == outputName) {
			OutputHandle h = outputName.getOutputHandle(0.0);
			if (h != null)
				unit.setSubClass(h.getUnitType());
			return;
		}
	}

	public String getRenderText(double simTime) {
		if( outputName.getValue() == null )
			return formatText.getValue();

		try {
		OutputHandle out = outputName.getOutputHandle(simTime);
		if( out == null )
			return failText.getValue();

		if (out.isNumericValue()) {
			double d = out.getValueAsDouble(simTime, 0.0d, unit.getValue());
			return String.format(formatText.getValue(), d);
		}
		else {
			Object o = out.getValue(simTime, out.getReturnType());
			return String.format(formatText.getValue(), o);
		}
		}
		catch (Throwable e) {
			return failText.getValue();
		}
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
		renderText = newRenderText;

	}

	public String getCachedText() {
		return renderText;
	}

	public double getTextHeight() {
		return textHeight.getValue().doubleValue();
	}

}
