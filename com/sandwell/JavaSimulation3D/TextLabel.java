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

import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation3D.util.LabelShape;
import com.sandwell.JavaSimulation3D.util.Shape;

public class TextLabel extends DisplayEntity  {
	private String renderText;
	private final StringInput text;
	private final DoubleInput textHeight;
	private final StringInput fontName;
	protected int fontStyle;
	private final ColourInput fontColor;

	protected LabelShape reference;

	{
		text = new StringInput("Text", "Graphics", "abc");
		this.addInput(text, true, "Label");

		textHeight = new DoubleInput("TextHeight", "Graphics", 0.3d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(textHeight, true);

		fontName = new StringInput("FontName", "Graphics", "Verdana");
		this.addInput(fontName, true);

		fontColor = new ColourInput("FontColour", "Graphics", Shape.getPresetColor(Shape.COLOR_BLACK));
		this.addInput(fontColor, true, "FontColor");

		addEditableKeyword( "FontStyle",        "", "Plain",    false, "Graphics" );
	}

	public TextLabel() {
		fontStyle = Font.PLAIN;

		reference = new LabelShape(text.getValue(), fontColor.getValue());
		this.getModel().addChild( reference );
	}


	public void earlyInit() {
		super.earlyInit();
		this.enterRegion();
	}

	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "FontStyle".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 0, 1, 2, 3);
			fontStyle = Font.PLAIN;
			for (int i = 0; i < data.size(); i++) {
				if (data.get(i).equalsIgnoreCase("Bold")) {
					fontStyle += Font.BOLD;
				}
				if (data.get(i).equalsIgnoreCase("Italic")) {
					fontStyle += Font.ITALIC;
				}
			}

			return;
		}

		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	public void render(double time) {
		if (text.getValue() != renderText) {
			reference.setHeight(textHeight.getValue());
			reference.setFillColor(fontColor.getValue());
			reference.setFont(fontName.getValue(), fontStyle, 1);
			reference.setText(text.getValue());
			renderText = text.getValue();

			Point2d labSize = reference.getSize();
			Vector3d tmp = new Vector3d(labSize.x, labSize.y, 0.0d);
			this.setSize(tmp);
		}

		super.render(time);
	}

	// Textlabel draws itself without a scale, never set it.
	public void setScale( double x, double y, double z ) {}
}