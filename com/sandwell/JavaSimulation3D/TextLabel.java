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

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation3D.util.LabelShape;
import com.sandwell.JavaSimulation3D.util.Shape;

public class TextLabel extends DisplayEntity  {
	private String renderText;
	private String text;
	protected double textHeight;
	protected String fontName;
	protected int fontStyle;
	protected ColoringAttributes fontColor = Shape.getPresetColor(Shape.COLOR_BLACK);

	protected LabelShape reference;

	{
		addEditableKeyword( "Text",             "", "abc", false, "Graphics", "Label");
		addEditableKeyword( "TextHeight",       "", "",    false, "Graphics" );
		addEditableKeyword( "FontName",         "", "",    false, "Graphics" );
		addEditableKeyword( "FontColour",       "", "",    false, "Graphics", "FontColor" );
		addEditableKeyword( "FontStyle",        "", "",    false, "Graphics" );
	}

	public TextLabel() {
		text = "abc";
		textHeight = 0.3;
		fontName = "Verdana";
		fontStyle = Font.TRUETYPE_FONT + Font.PLAIN;

		reference = new LabelShape(text, Shape.getPresetColor(Shape.COLOR_WHITE));
		this.getModel().addChild( reference );
	}


	public void earlyInit() {
		super.earlyInit();
		this.enterRegion();
	}

	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		if ("Text".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 0, 1);
			if (data.size() == 0)
				this.setText("");
			else
				this.setText(data.get(0));

			return;
		}

		if( "TextHeight".equalsIgnoreCase( keyword ) ) {
			double size = Input.parseDouble(data.get(0), 0.0d, Double.POSITIVE_INFINITY);
			this.setTextHeight(size);
			return;
		}
		if( "FontName".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 0, 1);
			if (data.size() != 0)
				fontName = data.get(0);
			return;
		}
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
				if (data.get(i).equalsIgnoreCase("TrueType")) {
					fontStyle += Font.TRUETYPE_FONT;
				}
			}

			return;
		}
		if ("FontColour".equalsIgnoreCase(keyword)) {
			fontColor = Input.parseColour(data);
			return;
		}

		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	public void setText(String str) {
		if (!text.equals(str))
			text = str;
	}

	public void render(double time) {
		if (text != renderText) {
			reference.setHeight(textHeight);
			reference.setFillColor(fontColor);
			reference.setFont(fontName, fontStyle, 1);
			reference.setText(text);
			renderText = text;

			Point2d labSize = reference.getSize();
			Vector3d tmp = new Vector3d(labSize.x, labSize.y, 0.0d);
			this.setSize(tmp);
		}

		super.render(time);
	}

	public void setTextHeight( double h ) {
		textHeight = h;
	}

	// Textlabel draws itself without a scale, never set it.
	public void setScale( double x, double y, double z ) {}
}