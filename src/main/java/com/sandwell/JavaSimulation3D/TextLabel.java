/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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

import com.sandwell.JavaSimulation.DeprecatedInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

public class TextLabel extends DisplayEntity  {
	@Keyword(desc = "The static text to be displayed.  If spaces are included, enclose the text in single quotes.",
	         example = "TitleLabel Text { 'Example Simulation Model' }")
	private final StringInput text;

	@Keyword(desc = "The height of the font as displayed in the view window.",
	         example = "TitleLabel TextHeight { 15 m }")
	private final DoubleInput textHeight;

	private String renderText;

	{
		text = new StringInput("Text", "Fixed Text", "abc");
		this.addInput(text, true);

		textHeight = new DoubleInput("TextHeight", "Fixed Text", 0.3d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(textHeight, true);

		DeprecatedInput oldfontName = new DeprecatedInput("FontName", "Has been moved to TextModel");
		oldfontName.setFatal(false);
		this.addInput(oldfontName, false);

		DeprecatedInput oldfontcolor = new DeprecatedInput("FontColour", "Has been moved to TextModel");
		oldfontcolor.setFatal(false);
		this.addInput(oldfontcolor, false, "FontColor");

		DeprecatedInput oldfontstyle = new DeprecatedInput("FontStyle", "Has been moved to TextModel");
		oldfontstyle.setFatal(false);
		this.addInput(oldfontstyle, false);

		DeprecatedInput oldds = new DeprecatedInput("DropShadow", "Has been moved to TextModel");
		oldds.setFatal(false);
		this.addInput(oldds, false);

		DeprecatedInput olddscol = new DeprecatedInput("DropShadowColour", "Has been moved to TextModel");
		olddscol.setFatal(false);
		this.addInput(olddscol, false);

		DeprecatedInput olddsoff = new DeprecatedInput("DropShadowOffset", "Has been moved to TextModel");
		olddsoff.setFatal(false);
		this.addInput(olddsoff, false);
	}

	public TextLabel() {
	}

	public String getRenderText(double time) {
		return text.getValue();
	}

	/**
	 * This method updates the DisplayEntity for changes in the given input
	 */
	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == textHeight) {
			setGraphicsDataDirty();
		}
	}

	@Override
	public void updateGraphics(double time) {
		super.updateGraphics(time);

		// This is cached because PropertyLabel uses reflection to get this, so who knows how long it will take
		String newRenderText = getRenderText(time);
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