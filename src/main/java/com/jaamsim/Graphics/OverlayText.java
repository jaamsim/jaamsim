/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;

import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.units.Unit;

/**
 * OverylayText displays written text as a 2D overlay on a View window.
 * @author Harry King
 *
 */
public class OverlayText extends OverlayEntity {

	@Keyword(description = "The height of the font as displayed in the view window. Unit is in pixels.",
	         exampleList = {"15"})
	private final IntegerInput textHeight;

	@Keyword(description = "The fixed and variable text to be displayed. If spaces are included, "
	                     + "enclose the text in single quotes. If variable text is to be "
	                     + "displayed using the DataSource keyword, include the appropriate Java "
	                     + "format in the text, such as %s, %.6f, %.6g.",
	         exampleList = {"'Present speed = %.3f m/s'", "'Present State = %s'"})
	protected final StringInput formatText;

	@Keyword(description = "The unit type for the numerical value to be displayed as "
	                     + "variable text. Set to DimensionlessUnit if the variable text is "
	                     + "non-numeric, such as the state of a Server.",
	         exampleList = {"DistanceUnit", "DimensionlessUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The unit in which to express an expression that returns a numeric "
	                     + "value.",
	         exampleList = {"m/s"})
	protected final EntityInput<Unit> unit;

	@Keyword(description = "An expression that returns the variable text to be displayed. "
	                     + "The expression can return a number that will be formated as text, "
	                     + "or it can return text directly, such as the state of a Server. "
	                     + "An object that returns a number, such as a TimeSeries, can also "
	                     + "be entered.",
	         exampleList = {"[Queue1].AverageQueueTime", "[Server1].State",
	                        "'[Queue1].QueueLength + [Queue2].QueueLength'",
	                        "TimeSeries1"})
	protected final StringProvInput dataSource;

	@Keyword(description = "The text to display if there is any failure while formatting the "
	                     + "variable text or while evaluating the expression.",
	         exampleList = {"'Input Error'"})
	protected final StringInput failText;

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The font styles to be applied to the text, e.g. Bold, Italic. ",
	         exampleList = { "Bold" })
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the text, specified by a colour keyword or RGB values.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput fontColor;

	@Keyword(description = "If TRUE, then a drop shadow appears for the text.",
	         exampleList = { "TRUE" })
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour for the drop shadow, specified by a colour keyword or "
	                     + "RGB values.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput dropShadowColor;

	@Keyword(description = "The { x, y, z } coordinates of the drop shadow's offset, expressed "
	                     + "as a decimal fraction of the text height.",
	         exampleList = { "0.1 -0.1 0.001" })
	private final Vec3dInput dropShadowOffset;

	private String renderText;

	{
		textHeight = new IntegerInput("TextHeight", "Key Inputs", 15);
		textHeight.setValidRange(0, 1000);
		this.addInput(textHeight);

		formatText = new StringInput("Format", "Key Inputs", "");
		this.addInput(formatText);

		unitType = new UnitTypeInput("UnitType", "Key Inputs", null);
		this.addInput(unitType);

		unit = new EntityInput<>(Unit.class, "Unit", "Key Inputs", null);
		unit.setSubClass(null);
		this.addInput(unit);

		dataSource = new StringProvInput("DataSource", "Key Inputs", null);
		this.addInput(dataSource);
		this.addSynonym(dataSource, "OutputName");

		failText = new StringInput("FailText", "Key Inputs", "Input Error");
		this.addInput(failText);

		fontName = new StringChoiceInput("FontName", "Font", -1);
		fontName.setChoices(TextModel.validFontNames);
		fontName.setDefaultText("TextModel");
		this.addInput(fontName);

		fontColor = new ColourInput("FontColour", "Font", ColourInput.BLACK);
		fontColor.setDefaultText("TextModel");
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", "Font", new ArrayList<String>(0));
		fontStyle.setValidOptions(TextModel.validStyles);
		fontStyle.setCaseSensitive(false);
		fontStyle.setDefaultText("TextModel");
		this.addInput(fontStyle);

		dropShadow = new BooleanInput("DropShadow", "Font", false);
		dropShadow.setDefaultText("TextModel");
		this.addInput(dropShadow);

		dropShadowColor = new ColourInput("DropShadowColour", "Font", ColourInput.BLACK);
		dropShadowColor.setDefaultText("TextModel");
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", "Font", null);
		dropShadowOffset.setDefaultText("TextModel");
		this.addInput(dropShadowOffset);
	}

	public OverlayText() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			Class<? extends Unit> ut = unitType.getUnitType();
			dataSource.setUnitType(ut);
			unit.setSubClass(ut);
			return;
		}
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Set the displayed text to the entity's name
		InputAgent.applyArgs(this, "Format", this.getName());
	}

	public String getRenderText(double simTime) {

		// Only static text is to be displayed
		if( dataSource.getValue() == null )
			return formatText.getValue();

		// Dynamic text is to be displayed
		try {
			double siFactor = 1.0d;
			if (unit.getValue() != null)
				siFactor = unit.getValue().getConversionFactorToSI();
			return dataSource.getValue().getNextString(simTime, formatText.getValue(), siFactor);
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

	public int getTextHeight() {
		return textHeight.getValue();
	}

	public StringChoiceInput getFontNameInput() {
		return fontName;
	}

	public StringListInput getFontStyleInput() {
		return fontStyle;
	}

	public ColourInput getFontColorInput() {
		return fontColor;
	}

	public BooleanInput getDropShadowInput() {
		return dropShadow;
	}

	public ColourInput getDropShadowColorInput() {
		return dropShadowColor;
	}

	public Vec3dInput getDropShadowOffsetInput() {
		return dropShadowOffset;
	}

}
