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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringChoiceInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vec3dInput;

public class TextLabel extends DisplayEntity  {
	@Keyword(desc = "The static text to be displayed.  If spaces are included, enclose the text in single quotes.",
	         example = "TitleLabel Text { 'Example Simulation Model' }")
	private final StringInput text;

	@Keyword(desc = "The height of the font as displayed in the view window.",
	         example = "TitleLabel TextHeight { 15 m }")
	private final DoubleInput textHeight;

	@Keyword(desc = "The name of the font to be used for the label. The " +
	                "font name must be enclosed in single quotes.",
	         example = "TitleLabel FontName { 'Arial' }")
	private final StringChoiceInput fontName;

	@Keyword(desc = "A list of font styles to be applied to the label, e.g. Bold, Italic. ",
	         example = "TitleLabel FontStyle { Bold }  ")
	private final StringListInput fontStyle;

	@Keyword(desc = "The colour of the font, defined using a colour keyword or RGB values.",
	         example = "TitleLabel FontColor { Red }")
	private final ColourInput fontColor;

	@Keyword(desc = "A Boolean value.  If TRUE, then a drop shadow appears for the text label.",
	         example = "TitleLabel  DropShadow { TRUE }")
	private final BooleanInput dropShadow;

	@Keyword(desc = "The colour for the drop shadow, defined using a colour keyword or RGB values.",
	         example = "TitleLabel  DropShadowColour { red }")
	private final ColourInput dropShadowColor;

	@Keyword(desc = "A set of { x, y, z } numbers that define the offset in each direction of the drop shadow from the TextLabel.",
	         example = "TitleLabel  DropShadowOffset { 0.1 0.1 0.0 }")
	private final Vec3dInput dropShadowOffset;


	// These are package-private as OverlayTextLabel is currently re-using them.
	// TODO: refactor this
	static final int defFont;
	static ArrayList<String> validFontNames;
	static ArrayList<String> validStyles;

	private int style; // Font Style
	private String renderText;

	static {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[ ] fontNames = ge.getAvailableFontFamilyNames();
		Arrays.sort(fontNames);
		validFontNames = new ArrayList<String>(Arrays.asList(fontNames));
		int def = validFontNames.indexOf("Verdana");
		if (def > -1)
			defFont = def;
		else
			defFont = 0;

		validStyles = new ArrayList<String>();
		validStyles.add("BOLD");
		validStyles.add("ITALIC");
	}

	{
		text = new StringInput("Text", "Fixed Text", "abc");
		this.addInput(text, true);

		textHeight = new DoubleInput("TextHeight", "Fixed Text", 0.3d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(textHeight, true);

		fontName = new StringChoiceInput("FontName", "Fixed Text", defFont);
		fontName.setChoices(validFontNames);
		this.addInput(fontName, true);

		fontColor = new ColourInput("FontColour", "Fixed Text", ColourInput.BLACK);
		this.addInput(fontColor, true, "FontColor");

		fontStyle = new StringListInput("FontStyle", "Fixed Text", new StringVector());
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		this.addInput(fontStyle, true);

		dropShadow = new BooleanInput( "DropShadow", "Fixed Text", false );
		this.addInput( dropShadow, true );

		dropShadowColor = new ColourInput("DropShadowColour", "Fixed Text", ColourInput.BLACK);
		this.addInput(dropShadowColor, true, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", "Fixed Text", new Vec3d(-0.1,-0.1,0.0));
		this.addInput(dropShadowOffset, true);
	}

	public TextLabel() {
		style = Font.PLAIN;
	}

	public String getRenderText(double time) {
		return text.getValue();
	}

	/**
	 * This method updates the DisplayEntity for changes in the given input
	 */
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == fontStyle) {
			style = Font.PLAIN;
			for(String each: fontStyle.getValue() ) {
				if(each.equalsIgnoreCase("Bold") ) {
					style += Font.BOLD;
				}
				else if (each.equalsIgnoreCase("Italic")) {
					style += Font.ITALIC;
				}
			}
		}
		if (in == fontStyle ||
		    in == fontName ||
		    in == textHeight ||
		    in == fontColor ||
		    in == dropShadow ||
		    in == dropShadowColor ||
		    in == dropShadowOffset) {
			setGraphicsDataDirty();
		}
	}

	@Override
	public void updateGraphics(double time) {
		// This is cached because PropertyLabel uses reflection to get this, so who knows how long it will take
		String newRenderText = getRenderText(time);
		if (newRenderText.equals(renderText)) {
			// Nothing important has changed
			return;
		}

		// The text has been updated
		setGraphicsDataDirty();
		renderText = newRenderText;

		// Update the size if we're running the new renderer (this will all be cleaned up eventually)
		if (RenderManager.isGood()) {

			TessFontKey key = new TessFontKey(fontName.getChoice(), style);
			Vec3d stringSize = RenderManager.inst().getRenderedStringSize(key, getTextHeight(), renderText);
			setSize(stringSize);
		}

	}

	public String getCachedText() {
		return renderText;
	}

	public String getFontName() {
		return fontName.getChoice();
	}

	public double getTextHeight() {
		return textHeight.getValue().doubleValue();
	}

	public Color4d getFontColor() {
		return fontColor.getValue();
	}

	public int getFontStyle() {
		return style;
	}

	public boolean getDropShadow() {
		return dropShadow.getValue();
	}
	public Color4d getDropShadowColor() {
		return dropShadowColor.getValue();
	}
	public Vec3d getDropShadowOffset() {
		return dropShadowOffset.getValue();
	}

}