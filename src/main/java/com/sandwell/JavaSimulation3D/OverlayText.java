/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.OutputInput;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

/**
 * OverylayText displays written text as a 2D overlay on a View window.
 * @author Harry King
 *
 */
public class OverlayText extends OverlayEntity {

	@Keyword(description = "The fixed and variable text to be displayed.  If spaces are included, enclose the text in single quotes.  " +
			"If variable text is to be displayed using the OutputName keyword, include the appropriate Java format in the text, " +
			"e.g. %s, %.6f, %.6g",
	         example = "OverlayText-1 Format { 'Present speed = %.3f m/s' }")
	protected final StringInput formatText;

	@Keyword(description = "The output value chain that returns the variable text to be displayed. " +
			"If more than one output value is given, all outputs but the last should point to an entity output to query" +
			" for the next output. The example returns the name of the product in a tank",
	         example = "OverlayText-1 OutputName { Tank1 Product Name }")
	protected final OutputInput<Object> outputName;

	@Keyword(description = "The unit inwhich to express the output value",
	         example = "OverlayText-1 Unit { m/s }")
	protected final EntityInput<Unit> unit;

	@Keyword(description = "The height of the font as displayed in the view window. Unit is in pixels.",
	         example = "OverlayText-1 TextHeight { 15 }")
	private final IntegerInput textHeight;

	@Keyword(description = "The text to display if there is any failure while formatting" +
	                       "the dynamic text, or while reading the output's value.",
	             example = "Text-1 FailText { '' }")
	private final StringInput failText;

	private String renderText;

	{
		formatText = new StringInput("Format", "Key Inputs", "abc");
		this.addInput(formatText, true);

		outputName = new OutputInput<Object>(Object.class, "OutputName", "Key Inputs", null);
		this.addInput(outputName, true);

		unit = new EntityInput<Unit>( Unit.class, "Unit", "Key Inputs", null);
		this.addInput(unit, true);

		textHeight = new IntegerInput("TextHeight", "Key Inputs", 15);
		textHeight.setValidRange(0, 1000);
		this.addInput(textHeight, true);

		failText = new StringInput("FailText", "Key Inputs", "");
		this.addInput(failText, true);
	}

	public String getRenderText(double simTime) {

		if( outputName.getValue() == null )
			return formatText.getValue();

		OutputHandle out = outputName.getOutputHandle(simTime);
		if( out == null )
			return failText.getValue();
		String ret = out.getValueAsString(simTime, unit.getValue(), formatText.getValue());
		if( ret == null )
			return failText.getValue();
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

	public int getTextHeight() {
		return textHeight.getValue();
	}

}
