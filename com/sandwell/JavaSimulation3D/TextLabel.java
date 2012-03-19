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

import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringChoiceInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vector3dInput;
import com.sandwell.JavaSimulation3D.util.LabelShape;
import com.sandwell.JavaSimulation3D.util.Shape;

public class TextLabel extends DisplayEntity  {
	private final StringInput text;
	private final DoubleInput textHeight;
	private final StringChoiceInput fontName;
	private static ArrayList<String> validFontNames;
	private static final int defFont;
	private final StringListInput fontStyle;
	private static ArrayList<String> validStyles;
	private int style; // Font Style
	private final ColourInput fontColor;
	private final BooleanInput dropShadow;
	private final ColourInput dropShadowColor;
	private final Vector3dInput dropShadowOffset;  // offset of drop shadow by fraction of text height

	protected LabelShape reference;
	protected LabelShape shadow;

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
		text = new StringInput("Text", "Graphics", "abc");
		this.addInput(text, true, "Label");

		textHeight = new DoubleInput("TextHeight", "Graphics", 0.3d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(textHeight, true);

		fontName = new StringChoiceInput("FontName", "Graphics", defFont);
		fontName.setChoices(validFontNames);
		this.addInput(fontName, true);

		fontColor = new ColourInput("FontColour", "Graphics", Shape.getPresetColor(Shape.COLOR_BLACK));
		this.addInput(fontColor, true, "FontColor");

		fontStyle = new StringListInput("FontStyle", "Graphics", new StringVector());
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		this.addInput(fontStyle, true);

		dropShadow = new BooleanInput( "DropShadow", "Graphics", false );
		this.addInput( dropShadow, true );

		dropShadowColor = new ColourInput("DropShadowColour", "Graphics", Shape.getPresetColor(Shape.COLOR_BLACK));
		this.addInput(dropShadowColor, true, "DropShadowColor");

		dropShadowOffset = new Vector3dInput("DropShadowOffset", "Graphics", new Vector3d(-0.1,-0.1,0.0));
		this.addInput(dropShadowOffset, true);
	}

	public TextLabel() {
		style = Font.PLAIN;
		reference = new LabelShape("", fontColor.getValue());
		shadow = new LabelShape("", dropShadowColor.getValue());
		this.getModel().addChild( shadow );
		this.getModel().addChild( reference );
	}

	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	String getRenderText(double time) {
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

		if( in == text ||
			in == textHeight ||
			in == fontName ||
			in == fontColor ||
			in == dropShadow ||
			in == dropShadowColor ||
			in == dropShadowOffset ||
			in == fontStyle ) {
			modelNeedsRender = true;
		}
	}

	public void render(double time) {
		if ( getRenderText(time) != (reference.getText()) ||
			modelNeedsRender ) {

			reference.setHeight(textHeight.getValue());
			reference.setFillColor(fontColor.getValue());
			reference.setFont(fontName.getChoice(), style, 1);
			reference.setText(getRenderText(time));

			if( dropShadow.getValue() ) {
				shadow.setHeight(textHeight.getValue());
				shadow.setFillColor(dropShadowColor.getValue());
				shadow.setFont(fontName.getChoice(), style, 1);
				shadow.setText(getRenderText(time));

				// Offset the shadow by fraction of text height
				shadow.setPos( textHeight.getValue() * dropShadowOffset.getValue().getX(),
						       textHeight.getValue() * dropShadowOffset.getValue().getY(),
						       textHeight.getValue() * dropShadowOffset.getValue().getZ() );
			}
			else {
				shadow.setText("");
			}

			Vector3d tmp = new Vector3d();
			reference.getSize(tmp);
			this.setSize(tmp);

			modelNeedsRender = false;
		}

		super.render(time);
	}

	// Textlabel draws itself without a scale, never set it.
	public void setScale( double x, double y, double z ) {}
}