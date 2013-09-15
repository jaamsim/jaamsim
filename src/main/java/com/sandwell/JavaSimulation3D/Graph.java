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
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.OutputListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DistanceUnit;
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
		public int removedPoints; // The number of points to draw for entities that have been removed
		public OutputHandle out; // The source of the data for the series
		public boolean isRemoved = false; // Is this line slated for removal (entity is dead)
		public double lineWidth;
		public Color4d lineColour;
	}

	protected final ArrayList<SeriesInfo> primarySeries;
	protected final ArrayList<SeriesInfo> secondarySeries;

	private Class<? extends Unit> dataUnitType;          // unit type for the graphed lines plotted against the y-axis
	private Class<? extends Unit> secondaryDataUnitType;  // unit type for the graphed lines plotted against the secondary y-axis

	// Size and position of the graph area (excluding the titles, labels, etc.) scaled to the unit cube for the DisplayModel
	protected Vec3d graphSize;   // graph size
	protected Vec3d graphOrigin; // bottom left position of the graph area,
	protected Vec3d graphCenter; // Center point of the graph area

	static int ENTITY_ONLY = 0;
	static int PARAMETER_ONLY = 1;
	static int ENTITY_PARAMETER = 2;

	// Data category

	@Keyword(description = "The number of data points that can be displayed on the graph. This " +
	                "parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final IntegerInput numberOfPoints;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"  Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 DataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> dataSource;

	@Keyword(description = "A list of colours (each consisting of a colour keyword or RGB values) for the line series to be displayed. " +
	                "For multiple colours, each colour must be enclosed in braces as they can themselves be defined as a list of RGB values.",
	         example = "Graph1 LineColors { midnightblue }")
	protected final ColorListInput lineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.",
	         example = "Graph1 LineWidths { 1 2 3 7 }")
	protected final DoubleListInput lineWidths;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"  Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 SecondaryDataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> secondaryDataSource;

	@Keyword(description = "A list of colours (each consisting of a colour keyword or RGB values) for the line series to be displayed. " +
	                "For multiple colours, each colour must be enclosed in braces as they can themselves be defined as a list of RGB values.",
	         example = "Graph1 SecondaryLineColors { midnightblue }")
	protected final ColorListInput secondaryLineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.",
	         example = "Graph1 SecondaryLineWidths { 1 2 3 7 }")
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
	private final StringInput xAxisLabelFormat;

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
	private final StringInput yAxisLabelFormat;

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
	         example = "Graph1 SecondaryYAxisUnit { t/h }")
	private final EntityInput<? extends Unit> secondaryYAxisUnit;

	@Keyword(description = "The minimum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisStart { 0 }")
	private final ValueInput secondaryYAxisStart;

	@Keyword(description = "The maximum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisEnd { 5 }")
	private final ValueInput secondaryYAxisEnd;

	@Keyword(description = "The interval between secondary y-axis labels.",
	         example = "Graph1 SecondaryYAxisInterval { 1 }")
	private final ValueInput secondaryYAxisInterval;

	@Keyword(description  = "The Java format to be used for the tick mark values on the secondary y-axis.\n" +
			"For example, the format %.1f would dispaly the value 5 as 5.0.",
	         example = "Graph1 SecondaryYAxisLabelFormat { %.1f }")
	private final StringInput secondaryYAxisLabelFormat;

	// Layout category

	@Keyword(description= "Text for the graph title, enclosed in single quotes if it contains spaces.",
	         example = "Graph1 Title { 'Title of the Graph' }")
	private final StringInput title;

	@Keyword(description = "The text height for the graph title.",
	         example = "Graph1 TitleTextHeight { 0.50 m }")
	private final ValueInput titleTextHeight;

	@Keyword(description = "The text height for the y-axis title.",
	         example = "Graph1 YAxisTitleTextHeight { 0.30 m }")
	private final ValueInput yAxisTitleTextHeight;

	@Keyword(description = "The text height for both x- and y-axis labels.",
	         example = "Graph1 LabelTextHeight { 0.35 m }")
	private final ValueInput labelTextHeight;

	@Keyword(description = "The gap between the title and top of the graph.",
	         example = "Graph1 TitleGap { 0.30 m }")
	private final ValueInput titleGap;

	@Keyword(description = "The gap between the x-axis labels and the x-axis.",
	         example = "Graph1 XAxisLabelGap { 0.30 m }")
	private final ValueInput xAxisLabelGap;

	@Keyword(description = "The gap between the y-axis and its labels.  If left blank, this is automatically calculated.",
	         example = "Graph1 YAxisLabelGap { 0.30 m }")
	private final ValueInput yAxisLabelGap;

	@Keyword(description = "The gap between the y-axis title and the y-axis labels.",
	         example = "Graph1 yAxisTitleGap { 0.30 m }")
	private final ValueInput yAxisTitleGap;

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 TopMargin { 0.30 m }")
	private final ValueInput topMargin;

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
             example = "Graph1 BottomMargin { 0.30 m }")
	private final ValueInput bottomMargin;

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 LeftMargin { 0.20 m }")
	private final ValueInput leftMargin;

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 RightMargin { 0.40 m }")
	private final ValueInput rightMargin;

	@Keyword(description = "The font name for all labels, enclosed in single quotes.",
	         example = "Graph1 LabelFontName { 'Arial' }")
	protected final StringInput labelFontName;

	@Keyword(description = "The colour of the graph background, defined by a color keyword or RGB values.",
	         example = "Graph1 GraphColor { floralwhite }")
	private final ColourInput graphColor;

	@Keyword(description = "The colour for both axes labels, defined using a colour keyword or RGB values.",
	         example = "Graph1 LabelFontColor { black }")
	private final ColourInput labelFontColor;

	@Keyword(description = "The color for the outer pane background, defined using a colour keyword or RGB values.",
	         example = "Graph1 BackgroundColor { floralwhite }")
	private final ColourInput backgroundColor;

	@Keyword(description = "The color of the graph border, defined using a colour keyword or RGB values.",
	         example = "Graph1 BorderColor { red }")
	private final ColourInput borderColor;

	@Keyword(description = "The colour for the graph title, defined by a color keyword or RGB values.",
	         example = "Graph1 TitleColor { black }")
	private final ColourInput titleColor;

	/*@Keyword(description = "The color for tick marks, defined using a colour keyword or RGB values.",
	         example = "Graph1 TickColor { black }")
	private final ColourInput tickColor;*/

	// Legend category (not implemented at present)

	/*@Keyword(description = "Coordinates (in { x, y, z }) of the center of the legend.",
	         example = "Graph1 LegendCenter { -10 -10 0 }")
	private final Vec3dInput legendCenter;

	@Keyword(description = "Size (width and height) of the legend.",
	         example = "Graph1 LegendSize { 7.00 4.00 }")
	private final Vec3dInput legendSize;

	@Keyword(description = "The height of the legend text.",
	         example = "Graph1 LegendTextHeight { 0.5 }")
	private DoubleInput legendTextHeight;

	@Keyword(description = "Width and height of the legend markers.",
	         example = "Graph1 LegendMarkerSize { 2.4 0.03 }")
	private final Vec3dInput legendMarkerSize;

	@Keyword(description = "The gap between the left margin of the legend and the text labels.",
	         example = "Graph1 LegendSeriesLabelGap { 3 }")
	private final DoubleInput seriesLabelGap;

	@Keyword(description = "The gap between the left margin of the legend and the legend markers.",
	         example = "Graph1 LegendSeriesMarkerGap { 0.1 }")
	private final DoubleInput seriesMakerGap; */

	{
		// Data category

		numberOfPoints = new IntegerInput("NumberOfPoints", "Data", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints, true);

		dataSource = new OutputListInput<Double>(Double.class, "DataSource", "Data", null);
		this.addInput(dataSource, true);

		lineColorsList = new ColorListInput("LineColours", "Data", new ArrayList<Color4d>(0));
		this.addInput(lineColorsList, true, "LineColors");

		lineWidths = new DoubleListInput("LineWidths", "Data", new DoubleVector());
		this.addInput(lineWidths, true);

		secondaryDataSource = new OutputListInput<Double>(Double.class, "SecondaryDataSource", "Data", null);
		this.addInput(secondaryDataSource, true);

		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", "Data", new ArrayList<Color4d>(0));
		this.addInput(secondaryLineColorsList, true, "SecondaryLineColors");

		secondaryLineWidths = new DoubleListInput("SecondaryLineWidths", "Data", new DoubleVector());
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

		xAxisLabelFormat = new StringInput("XAxisLabelFormat", "X-Axis", "%.0fs");
		this.addInput(xAxisLabelFormat, true);

		//tickColor = new ColourInput("TickColor", "X-Axis", ColourInput.DARK_BLUE);
		//this.addInput(tickColor, true, "TickColour");

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

		yAxisLabelFormat = new StringInput("YAxisLabelFormat", "Y-Axis", "%.1f");
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

		secondaryYAxisLabelFormat = new StringInput("SecondaryYAxisLabelFormat", "Secondary Y-Axis", "%.1f");
		this.addInput(secondaryYAxisLabelFormat, true);

		// Layout category

		title = new StringInput("Title", "Layout", "Graph Title");
		this.addInput(title, true);

		titleTextHeight = new ValueInput("TitleTextHeight", "Layout", 0.15d);
		titleTextHeight.setUnitType(DistanceUnit.class);
		this.addInput(titleTextHeight, true);

		yAxisTitleTextHeight = new ValueInput("YAxisTitleTextHeight", "Layout", 0.15d);
		yAxisTitleTextHeight.setUnitType(DistanceUnit.class);
		this.addInput(yAxisTitleTextHeight, true);

		labelTextHeight = new ValueInput("LabelTextHeight", "Layout", 0.07d);
		labelTextHeight.setUnitType(DistanceUnit.class);
		this.addInput(labelTextHeight, true);

		titleGap = new ValueInput("TitleGap", "Layout", 0.07d);
		titleGap.setUnitType(DistanceUnit.class);
		this.addInput(titleGap, true);

		xAxisLabelGap = new ValueInput("XAxisLabelGap", "Layout", 0.05d);
		xAxisLabelGap.setUnitType(DistanceUnit.class);
		this.addInput(xAxisLabelGap, true);

		yAxisTitleGap = new ValueInput("YAxisTitleGap", "Layout", 0.05d);
		yAxisTitleGap.setUnitType(DistanceUnit.class);
		this.addInput(yAxisTitleGap, true);

		yAxisLabelGap = new ValueInput("YAxisLabelGap", "Layout", 0.05d);
		yAxisLabelGap.setUnitType(DistanceUnit.class);
		this.addInput(yAxisLabelGap, true);

		topMargin = new ValueInput("TopMargin", "Layout", 0.30d);
		topMargin.setUnitType(DistanceUnit.class);
		this.addInput(topMargin, true);

		bottomMargin = new ValueInput("BottomMargin", "Layout", 0.30d);
		bottomMargin.setUnitType(DistanceUnit.class);
		this.addInput(bottomMargin, true);

		leftMargin = new ValueInput("LeftMargin", "Layout", 0.55d);
		leftMargin.setUnitType(DistanceUnit.class);
		this.addInput(leftMargin, true);

		rightMargin = new ValueInput("RightMargin", "Layout", 0.55d);
		rightMargin.setUnitType(DistanceUnit.class);
		this.addInput(rightMargin, true);

		labelFontName = new StringInput("LabelFontName", "Layout", "Verdana");
		this.addInput(labelFontName, true);

		titleColor = new ColourInput("TitleColor", "Layout", ColourInput.getColorWithName("brick"));
		this.addInput(titleColor, true, "TitleColour");

		labelFontColor = new ColourInput("LabelFontColor", "Layout", ColourInput.BLUE);
		this.addInput(labelFontColor, true, "LabelFontColour");

		graphColor = new ColourInput("GraphColor", "Layout", ColourInput.getColorWithName("ivory"));
		this.addInput(graphColor, true, "GraphColour");

		backgroundColor = new ColourInput("BackgroundColor", "Layout", ColourInput.getColorWithName("gray95"));
		this.addInput(backgroundColor, true, "BackgroundColour");

		borderColor = new ColourInput("BorderColor", "Layout", ColourInput.BLACK);
		this.addInput(borderColor, true, "BorderColour");

		// Legend category (not implemented at present)

		/*legendTextHeight = new DoubleInput("LegendTextHeight", "Legend", 0.5);
		this.addInput(legendTextHeight, true);

		seriesMakerGap = new DoubleInput("LegendSeriesMarkerGap", "Legend", 0.0);
		this.addInput(seriesMakerGap, true);

		seriesLabelGap = new DoubleInput("LegendSeriesLabelGap", "Legend", 0.0);
		this.addInput(seriesLabelGap, true);

		legendCenter = new Vec3dInput("LegendCenter", "Legend", new Vec3d(0.0d, 0.0d, 0.0d));
		this.addInput(legendCenter, true);

		legendSize = new Vec3dInput("LegendSize", "Legend", new Vec3d(1.0d, 1.0d, 0.0d));
		legendSize.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(legendSize, true);

		legendMarkerSize = new Vec3dInput("LegendMarkerSize", "Legend", new Vec3d(0.1d, 0.1d, 0.0d));
		legendMarkerSize.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(legendMarkerSize, true);*/
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

		if (in == xAxisLabelFormat) {
			String temp = xAxisLabelFormat.getValue();
			try {
				String.format(temp,0.0);
			}
			catch (Throwable e) {
				throw new InputErrorException("Invalid Java format string", temp);
			}
		}

		if (in == yAxisLabelFormat) {
			String temp = yAxisLabelFormat.getValue();
			try {
				String.format(temp,0.0);
			}
			catch (Throwable e) {
				throw new InputErrorException("Invalid Java format string", temp);
			}
		}

		if (in == secondaryYAxisLabelFormat) {
			String temp = secondaryYAxisLabelFormat.getValue();
			try {
				String.format(temp,0.0);
			}
			catch (Throwable e) {
				throw new InputErrorException("Invalid Java format string", temp);
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

	public Vec3d getGraphOrigin() {
		return graphOrigin;
	}

	public Vec3d getGraphSize() {
		return graphSize;
	}

	public Vec3d getGraphCenter() {
		return graphCenter;
	}

	@Override
	public void updateGraphics(double time) {
		super.updateGraphics(time);

		Vec3d graphExtent = getSize();
		// Draw graphic rectangle
		graphSize = new Vec3d();
		graphSize.x = ( graphExtent.x - leftMargin.getValue() - rightMargin.getValue() ) / graphExtent.x;
		graphSize.y = ( graphExtent.y - topMargin.getValue() - bottomMargin.getValue() ) / graphExtent.y;
		graphSize.z = 1;

		// Center position of the graph
		graphCenter = new Vec3d( ( leftMargin.getValue()/2 - rightMargin.getValue()/2 ) / graphExtent.x,
				( bottomMargin.getValue()/2 - topMargin.getValue()/2 ) / graphExtent.y , 0.0 );

		graphOrigin = new Vec3d( graphCenter.x - graphSize.x/2, graphCenter.y - graphSize.y/2, 0.0  );

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
			Color4d colour = getLineColor(i, lineColorsList.getValue());
			double lineWidth = getLineWidth(i, lineWidths);
			setupLine(info, colour, lineWidth);
		}

		for (int i = 0; i < secondarySeries.size(); ++i) {
			SeriesInfo info = secondarySeries.get(i);
			Color4d colour = getLineColor(i, secondaryLineColorsList.getValue());
			double lineWidth = getLineWidth(i, secondaryLineWidths);
			setupLine(info, colour, lineWidth);
		}

		Process.start(new ProcessGraphTarget(this));
	}

	/**
	 * Hook for sub-classes to do some processing at startup
	 */
	protected void extraStartGraph() {}

	protected void setupLine(SeriesInfo info, Color4d colour, double lineWidth) {
		info.lineWidth = lineWidth;
		info.lineColour = colour;
	}

	protected Color4d getLineColor(int index, ArrayList<Color4d> colorList) {
		Color4d currentLineColour = ColourInput.RED; // Default color
		if (colorList.size() >= index)
			currentLineColour = colorList.get(index);
		else if(colorList.size() == 1)
			currentLineColour = colorList.get(0);

		return currentLineColour;
	}

	protected double getLineWidth(int index, DoubleListInput widthList) {
		double lineWidth = 1.0d; // Default
		if (widthList.getValue().size() > index)
			lineWidth = widthList.getValue().get(index);
		else if (widthList.getValue().size() == 1)
			lineWidth = widthList.getValue().get(0);

		return lineWidth;
	}

	/**
	 * Initialize the data for the specified series
	 */
	private void setupSeriesData(SeriesInfo info, double xLength, double xInterval) {

		info.numPoints = 0;
		info.removedPoints = 0;

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

		// Initialize primary y-axis
		//for ()
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
	 * @param method - the method to call to gather more data points
	 */
	public void processGraph(SeriesInfo info) {

		// Entity has been removed
		if(info.out == null) {
			return;
		}

		double presentValue = 0;
		if (info.isRemoved) {
			presentValue = info.values[info.numPoints - 1];
		} else {
			presentValue = this.getCurrentValue( getSimTime() + endTime.getValue(), info);

		}

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
	public String getFontName() {
		return labelFontName.getValue();
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

	public Color4d getGraphColour() {
		return graphColor.getValue();
	}
	public Color4d getBorderColour() {
		return borderColor.getValue();
	}
	public Color4d getBackgroundColour() {
		return backgroundColor.getValue();
	}

	public Color4d getTitleColour() {
		return titleColor.getValue();
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

	public Color4d getLabelColour() {
		return labelFontColor.getValue();
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

	public double getTitleHeight() {
		return titleTextHeight.getValue() / getSize().y;
	}

	public double getTitleGap() {
		return titleGap.getValue() / getSize().y;
	}

	public double getLabelHeight() {
		return labelTextHeight.getValue() / getSize().y;
	}

	public double getXAxisLabelGap() {
		return xAxisLabelGap.getValue() / getSize().y;
	}

	public double getYAxisTitleHeight() {
		return yAxisTitleTextHeight.getValue() / getSize().x;
	}

	public double getYAxisTitleGap() {
		return yAxisTitleGap.getValue() / getSize().x;
	}

	public double getYAxisLabelGap() {
		return yAxisLabelGap.getValue() / getSize().x;
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
