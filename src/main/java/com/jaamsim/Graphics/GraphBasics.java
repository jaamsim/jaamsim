/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.FormatInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public abstract class GraphBasics extends DisplayEntity {

	/**
	 * A struct containing all the information pertaining to a specific series
	 */
	public static class SeriesInfo {
		public double[] yValues;
		public double[] xValues;
		public int numPoints; // number of points to be graphed
		public int indexOfLastEntry; // index in the arrays for the last graph point in the series
		public SampleProvider samp; // The source of the data for the series
		public double lineWidth;
		public Color4d lineColour;
	}

	protected final ArrayList<SeriesInfo> primarySeries;
	protected final ArrayList<SeriesInfo> secondarySeries;

	protected boolean timeTrace;  // TRUE if the x-axis moves so that zero is always the present time
	protected boolean showSecondaryYAxis;  // TRUE if the secondary y-axis is to be displayed

	// Key Inputs category

	@Keyword(description= "Text for the graph title.",
	         exampleList = {"'Title of the Graph'"})
	private final StringInput title;

	// X-Axis category

	@Keyword(description = "Title of the x-axis.",
	         exampleList = {"'Time (s)'"})
	private final StringInput xAxisTitle;

	@Keyword(description = "The unit to be used for the x-axis.",
	         exampleList = {"h"})
	private final EntityInput<Unit> xAxisUnit;

	@Keyword(description = "The minimum value for the x-axis.",
	         exampleList = {"-48 h"})
	protected final ValueInput xAxisStart;

	@Keyword(description = "The maximum value for the x-axis.",
	         exampleList = {"8 h"})
	protected final ValueInput xAxisEnd;

	@Keyword(description = "The interval between x-axis labels.",
	         exampleList = {"8 h"})
	private final ValueInput xAxisInterval;

	@Keyword(description = "The Java format to be used for the tick mark values on the x-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         exampleList = {"%.1f"})
	private final FormatInput xAxisLabelFormat;

	@Keyword(description = "A list of values between XAxisStart and XAxisEnd at which to insert vertical gridlines.",
	         exampleList = {"-48 -40 -32 -24 -16 -8 0 h"})
	private final ValueListInput xLines;

	@Keyword(description = "The color of the vertical gridlines (or a list corresponding to the colour of each " +
	                "gridline defined in XLines), defined using a colour keyword or RGB values.",
	         exampleList = {"gray76"})
	private final ColorListInput xLinesColor;

	// Y-Axis category

	@Keyword(description = "Title of the y-axis.",
	         exampleList = {"'Water Height (m)'"})
	private final StringInput yAxisTitle;

	@Keyword(description = "The unit to be used for the y-axis.\n" +
			"The unit chosen must be consistent with the unit type for the DataSource value,\n" +
			"i.e. if the data has units of distance, then unit must be a distance unit such as meters.",
	         exampleList = {"t/h"})
	private final EntityInput<Unit> yAxisUnit;

	@Keyword(description = "The minimum value for the y-axis.",
	         exampleList = {"0 t/h"})
	private final ValueInput yAxisStart;

	@Keyword(description = "The maximum value for the y-axis.",
	         exampleList = {"5 t/h"})
	private final ValueInput yAxisEnd;

	@Keyword(description = "The interval between y-axis labels.",
	         exampleList = {"1 t/h"})
	private final ValueInput yAxisInterval;

	@Keyword(description  = "The Java format to be used for the tick mark values on the y-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         exampleList = {"%.1f"})
	private final FormatInput yAxisLabelFormat;

	@Keyword(description = "A list of values between YAxisStart and YAxisEnd at which to insert horizontal gridlines.",
	         exampleList = {"0  0.5  1  1.5  2  2.5  3  t/h"})
	private final ValueListInput yLines;

	@Keyword(description = "The colour of the horizontal gridlines (or a list corresponding to the colour of each " +
                    "gridline defined in YLines), defined using a colour keyword or RGB values.",
	         exampleList = {"gray76"})
	private final ColorListInput yLinesColor;

	// Secondary Y-Axis category

	@Keyword(description = "Title of the secondary y-axis.",
	         exampleList = {"'Water Height (m)'"})
	private final StringInput secondaryYAxisTitle;

	@Keyword(description = "The unit to be used for the secondary y-axis.\n" +
			"The unit chosen must be consistent with the unit type for the DataSource value,\n" +
			"i.e. if the data has units of distance, then unit must be a distance unit such as meters.",
	         exampleList = {"m"})
	private final EntityInput<Unit> secondaryYAxisUnit;

	@Keyword(description = "The minimum value for the secondary y-axis.",
	         exampleList = {"0 m"})
	private final ValueInput secondaryYAxisStart;

	@Keyword(description = "The maximum value for the secondary y-axis.",
	         exampleList = {"5 m"})
	private final ValueInput secondaryYAxisEnd;

	@Keyword(description = "The interval between secondary y-axis labels.",
	         exampleList = {"1 m"})
	private final ValueInput secondaryYAxisInterval;

	@Keyword(description  = "The Java format to be used for the tick mark values on the secondary y-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         exampleList = {"%.1f"})
	private final FormatInput secondaryYAxisLabelFormat;

	{
		// Key Inputs category

		title = new StringInput("Title", "Key Inputs", "Graph Title");
		this.addInput(title);

		// X-Axis category

		xAxisTitle = new StringInput("XAxisTitle", "X-Axis", "X-Axis Title");
		this.addInput(xAxisTitle);

		xAxisUnit = new EntityInput<>(Unit.class, "XAxisUnit", "X-Axis", null);
		this.addInput(xAxisUnit);

		xAxisStart = new ValueInput("XAxisStart", "X-Axis", -60.0d);
		xAxisStart.setUnitType(UserSpecifiedUnit.class);
		xAxisStart.setValidRange(Double.NEGATIVE_INFINITY, 1.0e-6);
		this.addInput(xAxisStart);

		xAxisEnd = new ValueInput("XAxisEnd", "X-Axis", 0.0d);
		xAxisEnd.setUnitType(UserSpecifiedUnit.class);
		xAxisEnd.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(xAxisEnd);

		xAxisInterval = new ValueInput("XAxisInterval", "X-Axis", 10.0d);
		xAxisInterval.setUnitType(UserSpecifiedUnit.class);
		xAxisInterval.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		this.addInput(xAxisInterval);

		xAxisLabelFormat = new FormatInput("XAxisLabelFormat", "X-Axis", "%.0f");
		this.addInput(xAxisLabelFormat);

		DoubleVector defXLines = new DoubleVector();
		defXLines.add(-20.0);
		defXLines.add(-40.0);
		xLines = new ValueListInput("XLines", "X-Axis", defXLines);
		xLines.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xLines);

		ArrayList<Color4d> defXlinesColor = new ArrayList<>(0);
		defXlinesColor.add(ColourInput.getColorWithName("gray50"));
		xLinesColor = new ColorListInput("XLinesColor", "X-Axis", defXlinesColor);
		this.addInput(xLinesColor);
		this.addSynonym(xLinesColor, "XLinesColour");

		// Y-Axis category

		yAxisTitle = new StringInput("YAxisTitle", "Y-Axis", "Y-Axis Title");
		this.addInput(yAxisTitle);

		yAxisUnit = new EntityInput<>(Unit.class, "YAxisUnit", "Y-Axis", null);
		this.addInput(yAxisUnit);

		yAxisStart = new ValueInput("YAxisStart", "Y-Axis", 0.0);
		yAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisStart);

		yAxisEnd = new ValueInput("YAxisEnd", "Y-Axis", 5.0d);
		yAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisEnd);

		yAxisInterval = new ValueInput("YAxisInterval", "Y-Axis", 1.0d);
		yAxisInterval.setUnitType(UserSpecifiedUnit.class);
		yAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(yAxisInterval);

		yAxisLabelFormat = new FormatInput("YAxisLabelFormat", "Y-Axis", "%.1f");
		this.addInput(yAxisLabelFormat);

		DoubleVector defYLines = new DoubleVector();
		defYLines.add(1.0);
		defYLines.add(2.0);
		defYLines.add(3.0);
		defYLines.add(4.0);
		yLines = new ValueListInput("YLines", "Y-Axis", defYLines);
		yLines.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yLines);

		ArrayList<Color4d> defYlinesColor = new ArrayList<>(0);
		defYlinesColor.add(ColourInput.getColorWithName("gray50"));
		yLinesColor = new ColorListInput("YLinesColor", "Y-Axis", defYlinesColor);
		this.addInput(yLinesColor);
		this.addSynonym(yLinesColor, "YLinesColour");

		// Secondary Y-Axis category

		secondaryYAxisTitle = new StringInput("SecondaryYAxisTitle", "Secondary Y-Axis", "Secondary Y-Axis Title");
		this.addInput(secondaryYAxisTitle);

		secondaryYAxisUnit = new EntityInput<>(Unit.class, "SecondaryYAxisUnit", "Secondary Y-Axis", null);
		this.addInput(secondaryYAxisUnit);

		secondaryYAxisStart = new ValueInput("SecondaryYAxisStart", "Secondary Y-Axis", 0.0);
		secondaryYAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisStart);

		secondaryYAxisEnd = new ValueInput("SecondaryYAxisEnd", "Secondary Y-Axis", 5.0);
		secondaryYAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisEnd);

		secondaryYAxisInterval = new ValueInput("SecondaryYAxisInterval", "Secondary Y-Axis", 1.0);
		secondaryYAxisInterval.setUnitType(UserSpecifiedUnit.class);
		secondaryYAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(secondaryYAxisInterval);

		secondaryYAxisLabelFormat = new FormatInput("SecondaryYAxisLabelFormat", "Secondary Y-Axis", "%.1f");
		this.addInput(secondaryYAxisLabelFormat);
	}

	public GraphBasics() {

		primarySeries = new ArrayList<>();
		secondarySeries = new ArrayList<>();

		timeTrace = false;
		showSecondaryYAxis = false;
	}

	protected void setXAxisUnit(Class<? extends Unit> unitType) {
		xAxisUnit.setSubClass(unitType);
		xAxisStart.setUnitType(unitType);
		xAxisEnd.setUnitType(unitType);
		xAxisInterval.setUnitType(unitType);
		xLines.setUnitType(unitType);
		GUIFrame.updateUI();  // show the new units in the Input Editor
	}

	protected void setYAxisUnit(Class<? extends Unit> unitType) {
		yAxisUnit.setSubClass(unitType);
		yAxisStart.setUnitType(unitType);
		yAxisEnd.setUnitType(unitType);
		yAxisInterval.setUnitType(unitType);
		yLines.setUnitType(unitType);
		GUIFrame.updateUI();  // show the new units in the Input Editor
	}

	protected void setSecondaryYAxisUnit(Class<? extends Unit> unitType) {
		secondaryYAxisUnit.setSubClass(unitType);
		secondaryYAxisStart.setUnitType(unitType);
		secondaryYAxisEnd.setUnitType(unitType);
		secondaryYAxisInterval.setUnitType(unitType);
		GUIFrame.updateUI();  // show the new units in the Input Editor
	}

	public String getTitle() {
		return title.getValue();
	}

	public String getXAxisTitle() {
		return xAxisTitle.getValue();
	}

	public Unit getXAxisUnit() {
		return xAxisUnit.getValue();
	}

	public double getXAxisStart() {
		return xAxisStart.getValue();
	}

	public double getXAxisEnd() {
		return xAxisEnd.getValue();
	}

	public double getXAxisInterval() {
		return xAxisInterval.getValue();
	}

	public String getXAxisLabelFormat() {
		return xAxisLabelFormat.getValue();
	}

	public String getYAxisTitle() {
		return yAxisTitle.getValue();
	}

	public Unit getYAxisUnit() {
		return yAxisUnit.getValue();
	}

	public double getYAxisStart() {
		return yAxisStart.getValue();
	}

	public double getYAxisEnd() {
		return yAxisEnd.getValue();
	}

	public double getYAxisInterval() {
		return yAxisInterval.getValue();
	}

	public String getYAxisLabelFormat() {
		return yAxisLabelFormat.getValue();
	}

	public String getSecondaryYAxisTitle() {
		return secondaryYAxisTitle.getValue();
	}

	public Unit getSecondaryYAxisUnit() {
		return secondaryYAxisUnit.getValue();
	}

	public double getSecondaryYAxisStart() {
		return secondaryYAxisStart.getValue();
	}

	public double getSecondaryYAxisEnd() {
		return secondaryYAxisEnd.getValue();
	}

	public double getSecondaryYAxisInterval() {
		return secondaryYAxisInterval.getValue();
	}

	public String getSecondaryYAxisLabelFormat() {
		return secondaryYAxisLabelFormat.getValue();
	}

	public DoubleVector getXLines() {
		return xLines.getValue();
	}

	public ArrayList<Color4d> getXLineColours() {
		return xLinesColor.getValue();
	}

	public DoubleVector getYLines() {
		return yLines.getValue();
	}

	public ArrayList<Color4d> getYLineColours() {
		return yLinesColor.getValue();
	}

	public boolean getTimeTrace() {
		return timeTrace;
	}

	public boolean showSecondaryYAxis() {
		return showSecondaryYAxis;
	}

}
