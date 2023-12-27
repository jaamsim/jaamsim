/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2023 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.GraphModel;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.FormatInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public abstract class AbstractGraph extends DisplayEntity {

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
		public boolean isBar;
	}

	private final ArrayList<SeriesInfo> primarySeries;
	private final ArrayList<SeriesInfo> secondarySeries;

	// Key Inputs category

	@Keyword(description= "Text for the graph title.",
	         exampleList = {"'Title of the Graph'"})
	private final StringInput title;

	@Keyword(description = "The unit type for the primary y-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "The unit type for the secondary y-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput secondaryUnitType;

	// Format category

	@Keyword(description = "A list of colours for the lines graphed against the primary y-axis. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput lineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed. "
	                     + "If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput lineWidths;

	@Keyword(description = "A list of colours for the lines graphed against the secondary y-axis. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput secondaryLineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the seconardy line series to be displayed. "
	                     + "If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput secondaryLineWidths;

	// X-Axis category

	@Keyword(description = "Title of the x-axis.",
	         exampleList = {"'Time (s)'"})
	protected final StringInput xAxisTitle;

	@Keyword(description = "The unit to be used for the x-axis.",
	         exampleList = {"h"})
	protected final EntityInput<Unit> xAxisUnit;

	@Keyword(description = "The minimum value for the x-axis.",
	         exampleList = {"-48 h"})
	protected final ValueInput xAxisStart;

	@Keyword(description = "The maximum value for the x-axis.",
	         exampleList = {"8 h"})
	protected final ValueInput xAxisEnd;

	@Keyword(description = "The interval between x-axis labels.",
	         exampleList = {"8 h"})
	protected final ValueInput xAxisInterval;

	@Keyword(description = "The format to be used for the tick mark values on the x-axis.",
	         exampleList = {"%.1f"})
	protected final FormatInput xAxisLabelFormat;

	@Keyword(description = "A list of values between XAxisStart and XAxisEnd at which to insert "
	                     + "vertical gridlines.",
	         exampleList = {"-48 -40 -32 -24 -16 -8 0 h"})
	protected final ValueListInput xLines;

	@Keyword(description = "The colours for the vertical gridlines defined by input to the "
	                     + "'XLines' keyword. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"gray76"})
	protected final ColorListInput xLinesColor;

	// Y-Axis category

	@Keyword(description = "Title of the primary y-axis.",
	         exampleList = {"'Water Height (m)'"})
	private final StringInput yAxisTitle;

	@Keyword(description = "The unit to be used for the primary-axis.",
	         exampleList = {"t/h"})
	private final EntityInput<Unit> yAxisUnit;

	@Keyword(description = "The minimum value for the primary y-axis.",
	         exampleList = {"0 t/h"})
	private final ValueInput yAxisStart;

	@Keyword(description = "The maximum value for the primary y-axis.",
	         exampleList = {"5 t/h"})
	private final ValueInput yAxisEnd;

	@Keyword(description = "The interval between primary y-axis labels.",
	         exampleList = {"1 t/h"})
	private final ValueInput yAxisInterval;

	@Keyword(description = "The format to be used for the tick mark values on the primary y-axis.",
	         exampleList = {"%.1f"})
	private final FormatInput yAxisLabelFormat;

	@Keyword(description = "A list of values between YAxisStart and YAxisEnd at which to insert "
	                     + "horizontal gridlines.",
	         exampleList = {"0  0.5  1  1.5  2  2.5  3  t/h"})
	private final ValueListInput yLines;

	@Keyword(description = "The colours for the vertical gridlines defined by input to the "
	                     + "'YLines' keyword. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"gray76"})
	private final ColorListInput yLinesColor;

	// Secondary Y-Axis category

	@Keyword(description = "Title of the secondary y-axis.",
	         exampleList = {"'Water Height (m)'"})
	private final StringInput secondaryYAxisTitle;

	@Keyword(description = "The unit to be used for the secondary y-axis.",
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

	@Keyword(description = "The format to be used for the tick mark values on the secondary "
	                     + "y-axis.",
	         exampleList = {"%.1f"})
	private final FormatInput secondaryYAxisLabelFormat;

	public static final String X_AXIS = "X-Axis";
	public static final String Y_AXIS = "Y-Axis";
	public static final String SEC_Y_AXIS = "Secondary Y-Axis";

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(GraphModel.class);

		// Key Inputs category

		title = new StringInput("Title", KEY_INPUTS, "Graph Title");
		this.addInput(title);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, DimensionlessUnit.class);
		unitType.setCallback(unitTypeCallback);
		this.addInput(unitType);

		secondaryUnitType = new UnitTypeInput("SecondaryUnitType", KEY_INPUTS, DimensionlessUnit.class);
		secondaryUnitType.setCallback(secondaryUnitTypeCallback);
		this.addInput(secondaryUnitType);

		// Format category

		ArrayList<Color4d> defLineColor = new ArrayList<>(0);
		defLineColor.add(ColourInput.getColorWithName("red"));
		lineColorsList = new ColorListInput("LineColours", FORMAT, defLineColor);
		lineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		lineColorsList.setCallback(lineColoursCallback);
		this.addInput(lineColorsList);
		this.addSynonym(lineColorsList, "LineColors");

		lineWidths = new ValueListInput("LineWidths", FORMAT, 1.0d);
		lineWidths.setUnitType(DimensionlessUnit.class);
		lineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		lineWidths.setCallback(lineWidthsCallback);
		this.addInput(lineWidths);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", FORMAT, defSecondaryLineColor);
		secondaryLineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		secondaryLineColorsList.setCallback(secondaryLineColoursCallback);
		this.addInput(secondaryLineColorsList);
		this.addSynonym(secondaryLineColorsList, "SecondaryLineColors");

		secondaryLineWidths = new ValueListInput("SecondaryLineWidths", FORMAT, 1.0d);
		secondaryLineWidths.setUnitType(DimensionlessUnit.class);
		secondaryLineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		secondaryLineWidths.setCallback(secondaryLineWidthsCallback);
		this.addInput(secondaryLineWidths);

		// X-Axis category

		xAxisTitle = new StringInput("XAxisTitle", X_AXIS, "X-Axis Title");
		this.addInput(xAxisTitle);

		xAxisUnit = new EntityInput<>(Unit.class, "XAxisUnit", X_AXIS, null);
		this.addInput(xAxisUnit);

		xAxisStart = new ValueInput("XAxisStart", X_AXIS, -60.0d);
		xAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xAxisStart);

		xAxisEnd = new ValueInput("XAxisEnd", X_AXIS, 0.0d);
		xAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xAxisEnd);

		xAxisInterval = new ValueInput("XAxisInterval", X_AXIS, 10.0d);
		xAxisInterval.setUnitType(UserSpecifiedUnit.class);
		xAxisInterval.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		this.addInput(xAxisInterval);

		xAxisLabelFormat = new FormatInput("XAxisLabelFormat", X_AXIS, "%.0f");
		this.addInput(xAxisLabelFormat);

		xLines = new ValueListInput("XLines", X_AXIS, new DoubleVector(-20d, -40d));
		xLines.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xLines);

		ArrayList<Color4d> defXlinesColor = new ArrayList<>(0);
		defXlinesColor.add(ColourInput.getColorWithName("gray50"));
		xLinesColor = new ColorListInput("XLinesColor", X_AXIS, defXlinesColor);
		this.addInput(xLinesColor);
		this.addSynonym(xLinesColor, "XLinesColour");

		// Y-Axis category

		yAxisTitle = new StringInput("YAxisTitle", Y_AXIS, "Y-Axis Title");
		this.addInput(yAxisTitle);

		yAxisUnit = new EntityInput<>(Unit.class, "YAxisUnit", Y_AXIS, null);
		this.addInput(yAxisUnit);

		yAxisStart = new ValueInput("YAxisStart", Y_AXIS, 0.0);
		yAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisStart);

		yAxisEnd = new ValueInput("YAxisEnd", Y_AXIS, 5.0d);
		yAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisEnd);

		yAxisInterval = new ValueInput("YAxisInterval", Y_AXIS, 1.0d);
		yAxisInterval.setUnitType(UserSpecifiedUnit.class);
		yAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(yAxisInterval);

		yAxisLabelFormat = new FormatInput("YAxisLabelFormat", Y_AXIS, "%.1f");
		this.addInput(yAxisLabelFormat);

		yLines = new ValueListInput("YLines", Y_AXIS, new DoubleVector(1d, 2d, 3d, 4d));
		yLines.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yLines);

		ArrayList<Color4d> defYlinesColor = new ArrayList<>(0);
		defYlinesColor.add(ColourInput.getColorWithName("gray50"));
		yLinesColor = new ColorListInput("YLinesColor", Y_AXIS, defYlinesColor);
		this.addInput(yLinesColor);
		this.addSynonym(yLinesColor, "YLinesColour");

		// Secondary Y-Axis category

		secondaryYAxisTitle = new StringInput("SecondaryYAxisTitle", SEC_Y_AXIS, "Secondary Y-Axis Title");
		this.addInput(secondaryYAxisTitle);

		secondaryYAxisUnit = new EntityInput<>(Unit.class, "SecondaryYAxisUnit", SEC_Y_AXIS, null);
		this.addInput(secondaryYAxisUnit);

		secondaryYAxisStart = new ValueInput("SecondaryYAxisStart", SEC_Y_AXIS, 0.0);
		secondaryYAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisStart);

		secondaryYAxisEnd = new ValueInput("SecondaryYAxisEnd", SEC_Y_AXIS, 5.0);
		secondaryYAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisEnd);

		secondaryYAxisInterval = new ValueInput("SecondaryYAxisInterval", SEC_Y_AXIS, 1.0);
		secondaryYAxisInterval.setUnitType(UserSpecifiedUnit.class);
		secondaryYAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(secondaryYAxisInterval);

		secondaryYAxisLabelFormat = new FormatInput("SecondaryYAxisLabelFormat", SEC_Y_AXIS, "%.1f");
		this.addInput(secondaryYAxisLabelFormat);
	}

	public AbstractGraph() {
		primarySeries = new ArrayList<>();
		secondarySeries = new ArrayList<>();
	}

	@Override
	public void postDefine() {
		super.postDefine();
		setYAxisUnit(DimensionlessUnit.class);
		setSecondaryYAxisUnit(DimensionlessUnit.class);
		setXAxisUnit(DimensionlessUnit.class);
	}

	static final InputCallback unitTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Class<? extends Unit> ut = ((UnitTypeInput) inp).getUnitType();
			((AbstractGraph) ent).setYAxisUnit(ut);
		}
	};

	static final InputCallback secondaryUnitTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Class<? extends Unit> ut = ((UnitTypeInput) inp).getUnitType();
			((AbstractGraph) ent).setSecondaryYAxisUnit(ut);
		}
	};

	static final InputCallback lineColoursCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			AbstractGraph graph = (AbstractGraph) ent;
			for (int i = 0; i < graph.primarySeriesSize(); ++ i) {
				graph.setPrimarySeriesColour(i, graph.getLineColor(i));
			}
		}
	};

	static final InputCallback lineWidthsCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			AbstractGraph graph = (AbstractGraph) ent;
			for (int i = 0; i < graph.primarySeriesSize(); ++ i) {
				graph.setPrimarySeriesWidth(i, graph.getLineWidth(i));
			}
		}
	};

	static final InputCallback secondaryLineColoursCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			AbstractGraph graph = (AbstractGraph) ent;
			for (int i = 0; i < graph.secondarySeriesSize(); ++ i) {
				graph.setSecondarySeriesColour(i, graph.getSecondaryLineColor(i));
			}
		}
	};

	static final InputCallback secondaryLineWidthsCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			AbstractGraph graph = (AbstractGraph) ent;
			for (int i = 0; i < graph.secondarySeriesSize(); ++ i) {
				graph.setSecondarySeriesWidth(i, graph.getSecondaryLineWidth(i));
			}
		}
	};

	@Override
	public void earlyInit(){
		super.earlyInit();

		primarySeries.clear();
		secondarySeries.clear();
	}

	protected void setXAxisUnit(Class<? extends Unit> unitType) {
		xAxisUnit.setSubClass(unitType);
		xAxisStart.setUnitType(unitType);
		xAxisEnd.setUnitType(unitType);
		xAxisInterval.setUnitType(unitType);
		xLines.setUnitType(unitType);
	}

	protected void setYAxisUnit(Class<? extends Unit> unitType) {
		yAxisUnit.setSubClass(unitType);
		yAxisStart.setUnitType(unitType);
		yAxisEnd.setUnitType(unitType);
		yAxisInterval.setUnitType(unitType);
		yLines.setUnitType(unitType);
	}

	protected void setSecondaryYAxisUnit(Class<? extends Unit> unitType) {
		secondaryYAxisUnit.setSubClass(unitType);
		secondaryYAxisStart.setUnitType(unitType);
		secondaryYAxisEnd.setUnitType(unitType);
		secondaryYAxisInterval.setUnitType(unitType);
	}

	protected Color4d getLineColor(int index, ArrayList<Color4d> colorList) {
		index = Math.min(index, colorList.size()-1);
		return colorList.get(index);
	}

	protected int getLineWidth(int index, DoubleVector widthList) {
		index = Math.min(index, widthList.size()-1);
		return (int) widthList.get(index);
	}

	protected Color4d getLineColor(int index) {
		return getLineColor(index, lineColorsList.getValue());
	}

	protected Color4d getSecondaryLineColor(int index) {
		return getLineColor(index, secondaryLineColorsList.getValue());
	}

	protected int getLineWidth(int index) {
		return getLineWidth(index, lineWidths.getValue());
	}

	protected int getSecondaryLineWidth(int index) {
		return getLineWidth(index, secondaryLineWidths.getValue());
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

	public boolean isTimeTrace() {
		return false;
	}

	public boolean showSecondaryYAxis() {
		return false;
	}

	public ArrayList<SeriesInfo> getPrimarySeries() {
		return primarySeries;
	}

	public ArrayList<SeriesInfo> getSecondarySeries() {
		return secondarySeries;
	}

	protected void populatePrimarySeriesInfo(int numSeries, int numPoints, ArrayList<SampleProvider> sampList) {
		populateSeriesInfo(primarySeries, numSeries, numPoints, sampList);
	}

	protected void populateSecondarySeriesInfo(int numSeries, int numPoints, ArrayList<SampleProvider> sampList) {
		populateSeriesInfo(secondarySeries, numSeries, numPoints, sampList);
	}

	protected void populateSeriesInfo(ArrayList<SeriesInfo> infos, int numSeries, int numPoints, ArrayList<SampleProvider> sampList) {
		for (int i = 0; i < numSeries; ++i) {
			SeriesInfo info = new SeriesInfo();
			if (sampList != null)
				info.samp = sampList.get(i);
			info.yValues = new double[numPoints];
			info.xValues = new double[numPoints];

			infos.add(info);
		}
	}

	protected int primarySeriesSize() {
		return primarySeries.size();
	}

	protected int secondarySeriesSize() {
		return secondarySeries.size();
	}

	protected void setPrimarySeriesColour(int i, Color4d col) {
		if (i >= primarySeries.size())
			return;
		primarySeries.get(i).lineColour = col;
	}

	protected void setSecondarySeriesColour(int i, Color4d col) {
		if (i >= secondarySeries.size())
			return;
		secondarySeries.get(i).lineColour = col;
	}

	protected void setPrimarySeriesWidth(int i, int width) {
		if (i >= primarySeries.size())
			return;
		primarySeries.get(i).lineWidth = width;
	}

	protected void setSecondarySeriesWidth(int i, int width) {
		if (i >= secondarySeries.size())
			return;
		secondarySeries.get(i).lineWidth = width;
	}

}
