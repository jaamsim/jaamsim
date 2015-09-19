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
package com.jaamsim.Graphics;

import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.Unit;

/**
 * The "Text" object displays written text within the 3D model universe.  Both fixed and variable text can be displayed.
 * @author Harry King
 *
 */
public class Text extends TextBasics {

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
	private final StringInput failText;

	protected String renderText = "";

	{
		formatText = new StringInput("Format", "Key Inputs", "");
		this.addInput(formatText);

		unitType = new UnitTypeInput("UnitType", "Key Inputs", null);
		this.addInput(unitType);

		unit = new EntityInput<>( Unit.class, "Unit", "Key Inputs", null);
		unit.setSubClass(null);
		this.addInput(unit);

		dataSource = new StringProvInput("DataSource", "Key Inputs", null);
		this.addInput(dataSource);
		this.addSynonym(dataSource, "OutputName");

		failText = new StringInput("FailText", "Key Inputs", "Input Error");
		this.addInput(failText);
	}

	public Text() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == formatText) {
			setSavedText(formatText.getValue());
			return;
		}

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

	@Override
	public void acceptEdits() {
		super.acceptEdits();
		InputAgent.applyArgs(this, "Format", getEditText());
	}

	public String getRenderText(double simTime) {

		// If the object is selected, show the editable text
		if (isEditMode())
			return getEditText();

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

	@Override
	public String getCachedText() {
		return renderText;
	}

}
