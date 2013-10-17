/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2012 Ausenco Engineering Canada Inc.
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


import java.util.ArrayList;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.FormatInput;
import com.jaamsim.input.OutputListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.ColorListInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.StringInput;
import com.jaamsim.input.OutputHandle;

public class Graph extends DisplayEntity  {

	/**
	 * A struct containing all the information pertaining to a specific series
	 */
	public static class SeriesInfo {
		public double[] values;
		public int numPoints; // The first point to draw from the start (used to be confusingly called index)
		public OutputHandle out; // The source of the data for the series
		public double lineWidth;
		public Color4d lineColour;
	}

	protected final ArrayList<SeriesInfo> primarySeries;
	protected final ArrayList<SeriesInfo> secondarySeries;

	private Class<? extends Unit> dataUnitType;          // unit type for the graphed lines plotted against the y-axis
	private Class<? extends Unit> secondaryDataUnitType;  // unit type for the graphed lines plotted against the secondary y-axis

	static int ENTITY_ONLY = 0;
	static int PARAMETER_ONLY = 1;
	static int ENTITY_PARAMETER = 2;

	// Data category

	@Keyword(description= "Text for the graph title.",
	         example = "Graph1 Title { 'Title of the Graph' }")
	private final StringInput title;

	@Keyword(description = "The number of data points that can be displayed on the graph.\n" +
			" This parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final IntegerInput numberOfPoints;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 DataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> dataSource;

	@Keyword(description = "A list of colors for the line series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple lines, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the lines.",
	         example = "Graph1 LineColors { { red } { green } }")
	protected final ColorListInput lineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.\n" +
			"If only one line width is provided, it is used for all the lines.",
	         example = "Graph1 LineWidths { 2 1 }")
	protected final DoubleListInput lineWidths;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 SecondaryDataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> secondaryDataSource;

	@Keyword(description = "A list of colors for the secondary line series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple lines, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the lines.",
	         example = "Graph1 SecondaryLineColors { { red } { green } }")
	protected final ColorListInput secondaryLineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the seconardy line series to be displayed.\n" +
			"If only one line width is provided, it is used for all the lines.",
	         example = "Graph1 SecondaryLineWidths { 2 1 }")
	protected final DoubleListInput secondaryLineWidths;

	// X-Axis category

	@Keyword(description = "The time unit to be used for the x-axis.",
	         example = "Graph1 XAxisUnit { h }")
	private final EntityInput<TimeUnit> xAxisUnit;

	@Keyword(description = "The start time for the x-axis relative to the present time.\n" +
			"The present time is 0 for this axis.",
	         example = "Graph1 StartTime { -48 h }")
	protected final ValueInput startTime;

	@Keyword(description = "The end time for the x-axis relative to the present time.\n" +
			"The present time is 0 for this axis.",
	         example = "Graph1 EndTime { 8 h }")
	protected final ValueInput endTime;

	@Keyword(description = "The time increment between the tick marks on the x-axis.",
	         example = "Graph1 TimeInterval { 8 h }")
	private final ValueInput timeInterval;

	@Keyword(description = "The Java format to be used for the tick mark values on the x-axis.\n" +
			"For example, the format %.1fs would dispaly the value 5 as 5.0s.",
	         example = "Graph1 XAxisLabelFormat { %.1fs }")
	private final FormatInput xAxisLabelFormat;

	@Keyword(description = "A list of time values between StartTime and EndTime where vertical gridlines are inserted.",
	         example = "Graph1 XLines { -48 -40 -32 -24 -16 -8 0 h }")
	private final ValueListInput xLines;

	@Keyword(description = "The color of the vertical gridlines (or a list corresponding to the colour of each " +
	                "gridline defined in XLines), defined using a colour keyword or RGB values.",
	         example = "Graph1 XLinesColor { gray76 }")
	private final ColorListInput xLinesColor;

	// Y-Axis category

	@Keyword(description = "Title of the y-axis, enclosed in single quotes, rotated by 90 degrees counter-clockwise.",
	         example = "Graph1 YAxisTitle { 'Water Height (m)' }")
	private final StringInput yAxisTitle;

	@Keyword(description = "The unit to be used for the y-axis.\n" +
			"The unit chosen must be consistent with the unit type for the DataSource value,\n" +
			"i.e. if the data has units of distance, then unit must be a distance unit such as meters.",
	         example = "Graph1 YAxisUnit { t/h }")
	private final EntityInput<? extends Unit> yAxisUnit;

	@Keyword(description = "The minimum value for the y-axis.",
	         example = "Graph1 YAxisStart { 0 t/h }")
	private final ValueInput yAxisStart;

	@Keyword(description = "The maximum value for the y-axis.",
	         example = "Graph1 YAxisEnd { 5 t/h }")
	private final ValueInput yAxisEnd;

	@Keyword(description = "The interval between y-axis labels.",
	         example = "Graph1 YAxisInterval { 1 t/h }")
	private final ValueInput yAxisInterval;

	@Keyword(description  = "The Java format to be used for the tick mark values on the y-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         example = "Graph1 YAxisLabelFormat { %.1f }")
	private final FormatInput yAxisLabelFormat;

	@Keyword(description = "A list of values at which to insert horizontal gridlines.",
	         example ="Graph1 YLines { 0  0.5  1  1.5  2  2.5  3  t/h }")
	private final ValueListInput yLines;

	@Keyword(description = "The colour of the horizontal gridlines (or a list corresponding to the colour of each " +
                    "gridline defined in YLines), defined using a colour keyword or RGB values.",
	         example = "Graph1 YLinesColor { gray76 }")
	private final ColorListInput yLinesColor;

	// Secondary Y-Axis category

	@Keyword(description = "Title of the secondary y-axis, enclosed in single quotes, rotated by 90 degrees clockwise.",
	         example = "Graph1 SecondaryYAxisTitle { 'Water Height (m)' }")
	private final StringInput secondaryYAxisTitle;

	@Keyword(description = "The unit to be used for the secondary y-axis.\n" +
			"The unit chosen must be consistent with the unit type for the DataSource value,\n" +
			"i.e. if the data has units of distance, then unit must be a distance unit such as meters.",
	         example = "Graph1 SecondaryYAxisUnit { m }")
	private final EntityInput<? extends Unit> secondaryYAxisUnit;

	@Keyword(description = "The minimum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisStart { 0 m }")
	private final ValueInput secondaryYAxisStart;

	@Keyword(description = "The maximum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisEnd { 5 m }")
	private final ValueInput secondaryYAxisEnd;

	@Keyword(description = "The interval between secondary y-axis labels.",
	         example = "Graph1 SecondaryYAxisInterval { 1 m }")
	private final ValueInput secondaryYAxisInterval;

	@Keyword(description  = "The Java format to be used for the tick mark values on the secondary y-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         example = "Graph1 SecondaryYAxisLabelFormat { %.1f }")
	private final FormatInput secondaryYAxisLabelFormat;

	{
		// Data category

		title = new StringInput("Title", "Data", "Graph Title");
		this.addInput(title, true);

		numberOfPoints = new IntegerInput("NumberOfPoints", "Data", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints, true);

		dataSource = new OutputListInput<Double>(Double.class, "DataSource", "Data", null);
		this.addInput(dataSource, true);

		ArrayList<Color4d> defLineColor = new ArrayList<Color4d>(0);
		defLineColor.add(ColourInput.getColorWithName("red"));
		lineColorsList = new ColorListInput("LineColours", "Data", defLineColor);
		lineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineColorsList, true, "LineColors");

		DoubleVector defLineWidths = new DoubleVector();
		defLineWidths.add(1.0);
		lineWidths = new DoubleListInput("LineWidths", "Data", defLineWidths);
		lineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineWidths, true);

		secondaryDataSource = new OutputListInput<Double>(Double.class, "SecondaryDataSource", "Data", null);
		this.addInput(secondaryDataSource, true);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<Color4d>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", "Data", defSecondaryLineColor);
		secondaryLineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineColorsList, true, "SecondaryLineColors");

		DoubleVector defSecondaryLineWidths = new DoubleVector();
		defSecondaryLineWidths.add(1.0);
		secondaryLineWidths = new DoubleListInput("SecondaryLineWidths", "Data", defSecondaryLineWidths);
		secondaryLineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineWidths, true);

		// X-Axis category

		xAxisUnit = new EntityInput<TimeUnit>(TimeUnit.class, "XAxisUnit", "X-Axis", null);
		this.addInput(xAxisUnit, true);

		startTime = new ValueInput("StartTime", "X-Axis", -60.0d);
		startTime.setUnitType(TimeUnit.class);
		startTime.setValidRange(Double.NEGATIVE_INFINITY, 1.0e-6);
		this.addInput(startTime, true);

		endTime = new ValueInput("EndTime", "X-Axis", 0.0d);
		endTime.setUnitType(TimeUnit.class);
		endTime.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(endTime, true);

		timeInterval = new ValueInput("TimeInterval", "X-Axis", 10.0d);
		timeInterval.setUnitType(TimeUnit.class);
		timeInterval.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		this.addInput(timeInterval, true);

		xAxisLabelFormat = new FormatInput("XAxisLabelFormat", "X-Axis", "%.0fs");
		this.addInput(xAxisLabelFormat, true);

		DoubleVector defXLines = new DoubleVector();
		defXLines.add(-20.0);
		defXLines.add(-40.0);
		xLines = new ValueListInput("XLines", "X-Axis", defXLines);
		xLines.setUnitType(TimeUnit.class);
		this.addInput(xLines, true);

		ArrayList<Color4d> defXlinesColor = new ArrayList<Color4d>(0);
		defXlinesColor.add(ColourInput.getColorWithName("gray50"));
		xLinesColor = new ColorListInput("XLinesColor", "X-Axis", defXlinesColor);
		this.addInput(xLinesColor, true, "XLinesColour");

		// Y-Axis category

		yAxisTitle = new StringInput("YAxisTitle", "Y-Axis", "Y-Axis Title");
		this.addInput(yAxisTitle, true);

		yAxisUnit = new EntityInput<Unit>(Unit.class, "YAxisUnit", "Y-Axis", null);
		this.addInput(yAxisUnit, true);

		yAxisStart = new ValueInput("YAxisStart", "Y-Axis", 0.0);
		yAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisStart, true);

		yAxisEnd = new ValueInput("YAxisEnd", "Y-Axis", 5.0d);
		yAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yAxisEnd, true);

		yAxisInterval = new ValueInput("YAxisInterval", "Y-Axis", 1.0d);
		yAxisInterval.setUnitType(UserSpecifiedUnit.class);
		yAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(yAxisInterval, true);

		yAxisLabelFormat = new FormatInput("YAxisLabelFormat", "Y-Axis", "%.1f");
		this.addInput(yAxisLabelFormat, true);

		DoubleVector defYLines = new DoubleVector();
		defYLines.add(1.0);
		defYLines.add(2.0);
		defYLines.add(3.0);
		defYLines.add(4.0);
		yLines = new ValueListInput("YLines", "Y-Axis", defYLines);
		yLines.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yLines, true);

		ArrayList<Color4d> defYlinesColor = new ArrayList<Color4d>(0);
		defYlinesColor.add(ColourInput.getColorWithName("gray50"));
		yLinesColor = new ColorListInput("YLinesColor", "Y-Axis", defYlinesColor);
		this.addInput(yLinesColor, true, "YLinesColour");

		// Secondary Y-Axis category

		secondaryYAxisTitle = new StringInput("SecondaryYAxisTitle", "Secondary Y-Axis", "Secondary Y-Axis Title");
		this.addInput(secondaryYAxisTitle, true);

		secondaryYAxisUnit = new EntityInput<Unit>(Unit.class, "SecondaryYAxisUnit", "Secondary Y-Axis", null);
		this.addInput(secondaryYAxisUnit, true);

		secondaryYAxisStart = new ValueInput("SecondaryYAxisStart", "Secondary Y-Axis", 0.0);
		secondaryYAxisStart.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisStart, true);

		secondaryYAxisEnd = new ValueInput("SecondaryYAxisEnd", "Secondary Y-Axis", 5.0);
		secondaryYAxisEnd.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryYAxisEnd, true);

		secondaryYAxisInterval = new ValueInput("SecondaryYAxisInterval", "Secondary Y-Axis", 1.0);
		secondaryYAxisInterval.setUnitType(UserSpecifiedUnit.class);
		secondaryYAxisInterval.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		this.addInput(secondaryYAxisInterval, true);

		secondaryYAxisLabelFormat = new FormatInput("SecondaryYAxisLabelFormat", "Secondary Y-Axis", "%.1f");
		this.addInput(secondaryYAxisLabelFormat, true);
	}

	public Graph() {

		primarySeries = new ArrayList<SeriesInfo>();
		secondarySeries = new ArrayList<SeriesInfo>();
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == dataSource) {
			ArrayList<OutputHandle> outs = dataSource.getValue();
			if (outs.isEmpty())
				return;
			Class<? extends Unit> temp = outs.get(0).getUnitType();
			for (int i=1; i<outs.size(); i++) {
				if( outs.get(i).getUnitType() != temp )
					throw new InputErrorException("All inputs for keyword DataSource must have the same unit type./n" +
							"The unit type for the first source is %s", temp);
			}
			dataUnitType = temp;
			yAxisStart.setUnitType(dataUnitType);
			yAxisEnd.setUnitType(dataUnitType);
			yAxisInterval.setUnitType(dataUnitType);
			yLines.setUnitType(dataUnitType);
			FrameBox.valueUpdate();  // show the new units in the Input Editor
		}

		if (in == secondaryDataSource) {
			ArrayList<OutputHandle> outs = secondaryDataSource.getValue();
			if (outs.isEmpty())
				return;
			Class<? extends Unit> temp = outs.get(0).getUnitType();
			for (int i=1; i<outs.size(); i++) {
				if( outs.get(i).getUnitType() != temp )
					throw new InputErrorException("All inputs for keyword SecondaryDataSource must have the same unit type./n" +
							"The unit type for the first source is %s", temp);
			}
			secondaryDataUnitType = temp;
			secondaryYAxisStart.setUnitType(secondaryDataUnitType);
			secondaryYAxisEnd.setUnitType(secondaryDataUnitType);
			secondaryYAxisInterval.setUnitType(secondaryDataUnitType);
			FrameBox.valueUpdate();  // show the new units in the Input Editor
		}

		if (in == lineColorsList) {
			for (int i = 0; i < primarySeries.size(); ++ i) {
				SeriesInfo info = primarySeries.get(i);
				info.lineColour = getLineColor(i, lineColorsList.getValue());
			}
		}

		if (in == lineWidths) {
			for (int i = 0; i < primarySeries.size(); ++ i) {
				SeriesInfo info = primarySeries.get(i);
				info.lineWidth = getLineWidth(i, lineWidths.getValue());
			}
		}

		if (in == secondaryLineColorsList) {
			for (int i = 0; i < secondarySeries.size(); ++ i) {
				SeriesInfo info = secondarySeries.get(i);
				info.lineColour = getLineColor(i, secondaryLineColorsList.getValue());
			}
		}

		if (in == secondaryLineWidths) {
			for (int i = 0; i < secondarySeries.size(); ++ i) {
				SeriesInfo info = secondarySeries.get(i);
				info.lineWidth = getLineWidth(i, secondaryLineWidths.getValue());
			}
		}
	}

	@Override
	public void validate()
	throws InputErrorException {
		super.validate();

		if(yLinesColor.getValue().size() > 1) {
			Input.validateIndexedLists(yLines.getValue(), yLinesColor.getValue(), "YLines", "YLinesColor");
		}

		if(xLinesColor.getValue().size() > 1) {
			Input.validateIndexedLists(xLines.getValue(), xLinesColor.getValue(), "XLines", "XLinesColor");
		}

		if(lineColorsList.getValue().size() > 1){
			Input.validateIndexedLists(dataSource.getValue(), lineColorsList.getValue(),
					"DataSource", "LinesColor");
		}

		if(secondaryLineColorsList.getValue().size() > 1){
			Input.validateIndexedLists(secondaryDataSource.getValue(), secondaryLineColorsList.getValue(),
					"SecondaryTargetEntityList", "SecondaryLinesColor");
		}

		if(lineWidths.getValue().size() > 1)
			Input.validateIndexedLists(dataSource.getValue(), lineWidths.getValue(),
					"DataSource", "LineWidths");

		if(secondaryLineWidths.getValue().size() > 1)
			Input.validateIndexedLists(secondaryDataSource.getValue(), secondaryLineWidths.getValue(),
					"SecondaryDataSource", "SecondaryLineWidths");

		for( int i = 0; i < yLines.getValue().size(); i++ ) {
			double y = yLines.getValue().get( i );
			if( y > yAxisEnd.getValue() || y < yAxisStart.getValue() ) {
				throw new InputErrorException("value for yLines should be in (%f, %f) range -- it is (%f)",
					yAxisStart.getValue(), yAxisEnd.getValue(), y);
			}
		}

		for( int i = 0; i < xLines.getValue().size(); i++ ) {
			double x = xLines.getValue().get( i );
			if( x < startTime.getValue() || x > endTime.getValue() ) {
				throw new InputErrorException("value for xLines should be in (%f, %f) range -- it is (%f)",
					startTime.getValue(), endTime.getValue(), x);
			}
		}
	}

	@Override
	public void earlyInit(){
		super.earlyInit();

		primarySeries.clear();
		secondarySeries.clear();

		// Populate the primary series data structures
		populateSeriesInfo(primarySeries, dataSource);
		populateSeriesInfo(secondarySeries, secondaryDataSource);
	}

	private void populateSeriesInfo(ArrayList<SeriesInfo> infos, OutputListInput<Double> data) {
		ArrayList<OutputHandle> outs = data.getValue();
		if( outs == null )
			return;
		for (int outInd = 0; outInd < outs.size(); ++outInd) {
			SeriesInfo info = new SeriesInfo();
			info.out = outs.get(outInd);
			info.values = new double[numberOfPoints.getValue()];

			infos.add(info);
		}
	}

	private static class ProcessGraphTarget extends ProcessTarget {
		final Graph graph;

		ProcessGraphTarget(Graph graph) {
			this.graph = graph;
		}

		@Override
		public String getDescription() {
			return graph.getInputName() + ".processGraph";
		}

		@Override
		public void process() {
			graph.processGraph();
		}
	}

	@Override
	public void startUp() {
		super.startUp();
		extraStartGraph();

		for (int i = 0; i < primarySeries.size(); ++ i) {
			SeriesInfo info = primarySeries.get(i);
			info.lineColour = getLineColor(i, lineColorsList.getValue());
			info.lineWidth = getLineWidth(i, lineWidths.getValue());
		}

		for (int i = 0; i < secondarySeries.size(); ++i) {
			SeriesInfo info = secondarySeries.get(i);
			info.lineColour = getLineColor(i, secondaryLineColorsList.getValue());
			info.lineWidth = getLineWidth(i, secondaryLineWidths.getValue());
		}

		Process.start(new ProcessGraphTarget(this));
	}

	/**
	 * Hook for sub-classes to do some processing at startup
	 */
	protected void extraStartGraph() {}

	protected Color4d getLineColor(int index, ArrayList<Color4d> colorList) {
		if (colorList.size() == 1)
			return colorList.get(0);
		return colorList.get(index);
	}

	protected double getLineWidth(int index, DoubleVector widthList) {
		if (widthList.size() == 1)
			return widthList.get(0);
		return widthList.get(index);
	}

	/**
	 * Initialize the data for the specified series
	 */
	private void setupSeriesData(SeriesInfo info, double xLength, double xInterval) {

		info.numPoints = 0;

		for( int i = 0; i * xInterval < endTime.getValue(); i++ ) {
			double presentValue = this.getCurrentValue( i * xInterval, info);
			info.values[info.numPoints++] = presentValue;
		}
	}

	/**
	 * A hook method for descendant graph types to grab some processing time
	 */
	protected void extraProcessing() {}

	/**
	 * Calculate values for the data series on the graph
	 */
	public void processGraph() {
		if( traceFlag ) this.trace( "processGraph()" );

		double xLength = endTime.getValue() - startTime.getValue();
		double xInterval = xLength/(numberOfPoints.getValue() -1);

		for (SeriesInfo info : primarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		for (SeriesInfo info : secondarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		while ( true ) {

			// Give processing time to sub-classes
			extraProcessing();

			// Calculate values for the primary y-axis
			for (SeriesInfo info : primarySeries) {
				processGraph(info);
			}

			// Calculate values for the secondary y-axis
			for (SeriesInfo info : secondarySeries) {
				processGraph(info);
			}

			simWait( xInterval, 7 );
		}
	}

	/**
	 * Calculate values for the data series on the graph
	 * @param info - the information for the series to be rendered
	 */
	public void processGraph(SeriesInfo info) {

		// Entity has been removed
		if(info.out == null) {
			return;
		}

		double t = getSimTime() + endTime.getValue();
		double presentValue = this.getCurrentValue(t, info);
		if (info.numPoints < info.values.length) {
			info.values[info.numPoints++] = presentValue;
		}
		else {
			System.arraycopy(info.values, 1, info.values, 0, info.values.length - 1);
			info.values[info.values.length - 1] = presentValue;
		}
	}

	/**
	 * Return the current value for the series
	 * @return double
	 */
	protected double getCurrentValue(double simTime, SeriesInfo info) {
		return info.out.getValueAsDouble(simTime, 0.0);
	}

	public ArrayList<SeriesInfo> getPrimarySeries() {
		return primarySeries;
	}

	public ArrayList<SeriesInfo> getSecondarySeries() {
		return secondarySeries;
	}

	public DoubleVector getYLines() {
		return yLines.getValue();
	}

	public ArrayList<Color4d> getYLineColours() {
		return yLinesColor.getValue();
	}

	public DoubleVector getXLines() {
		return xLines.getValue();
	}

	public ArrayList<Color4d> getXLineColours() {
		return xLinesColor.getValue();
	}

	public double getStartTime() {
		return startTime.getValue();
	}

	public double getEndTime() {
		return endTime.getValue();
	}

	public String getTitle() {
		return title.getValue();
	}

	public double getYAxisStart() {
		return yAxisStart.getValue();
	}

	public double getYAxisEnd() {
		return yAxisEnd.getValue();
	}

	public double getSecondaryYAxisStart() {
		return secondaryYAxisStart.getValue();
	}

	public double getSecondaryYAxisEnd() {
		return secondaryYAxisEnd.getValue();
	}

	public int getNumberOfPoints() {
		return numberOfPoints.getValue();
	}

	public String getYAxisTitle() {
		return yAxisTitle.getValue();
	}

	public String getSecondaryYAxisTitle() {
		return secondaryYAxisTitle.getValue();
	}

	public double getYAxisInterval() {
		return yAxisInterval.getValue();
	}

	public String getYAxisLabelFormat() {
		return yAxisLabelFormat.getValue();
	}

	public double getSecondaryYAxisInterval() {
		return secondaryYAxisInterval.getValue();
	}

	public String getSecondaryYAxisLabelFormat() {
		return secondaryYAxisLabelFormat.getValue();
	}

	public String getXAxisLabelFormat() {
		return xAxisLabelFormat.getValue();
	}

	public TimeUnit getXAxisUnit() {
		return xAxisUnit.getValue();
	}

	public Unit getYAxisUnit() {
		return yAxisUnit.getValue();
	}

	public Unit getSecondaryYAxisUnit() {
		return secondaryYAxisUnit.getValue();
	}

	public double getTimeInterval() {
		return timeInterval.getValue();
	}

	// ******************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************

	/**
	 * Return the value for the given data point index for the given series index.
	 * @param seriesIndex - the index of the data series (starting from 1)
	 * @param pointIndex - the index of the data point (starting from 1)
	 */
	public double Series_Point( Integer seriesIndex, Integer pointIndex ) {
		return primarySeries.get(seriesIndex).values[pointIndex - 1];
	}
}
