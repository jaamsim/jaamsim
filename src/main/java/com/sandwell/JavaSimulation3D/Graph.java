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
import com.jaamsim.input.OutputListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.ColorListInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Vec3dInput;
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

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"  Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 DataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> dataSource;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"  Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 SecondaryDataSource { { Entity-1 Output-1 } { Entity-2 Output-2 } }")
	protected final OutputListInput<Double> secondaryDataSource;

	@Keyword(description = "The number of data points that can be displayed on the graph. This " +
	                "parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final IntegerInput numberOfPoints;	 // Total number of values that can be shown on the graph (the more the sharper the graph)

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


	@Keyword(description = "The minimum value for the y-axis.",
	         example = "Graph1 YAxisStart { 0 }")
	private final DoubleInput yAxisStart;

	@Keyword(description = "The maximum value for the y-axis.",
	         example = "Graph1 YAxisEnd { 5 }")
	private final DoubleInput yAxisEnd;

	@Keyword(description = "The interval between y-axis labels.",
	         example = "Graph1 YAxisInterval { 1 }")
	private final DoubleInput yAxisInterval;

	@Keyword(description = "A list of values at which to insert horizontal gridlines.",
	         example ="Graph1 YLines { 0 0.5 1 1.5 2 2.5 3 }")
	private final DoubleListInput yLines; // Horizontal lines

	@Keyword(description = "The colour of the horizontal gridlines (or a list corresponding to the colour of each " +
                    "gridline defined in YLines), defined using a colour keyword or RGB values.",
	         example = "Graph1 YLinesColor { gray76 }")
	private final ColorListInput yLinesColor;

	@Keyword(description = "The minimum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisStart { 0 }")
	private final DoubleInput secondaryYAxisStart;

	@Keyword(description = "The maximum value for the secondary y-axis.",
	         example = "Graph1 SecondaryYAxisEnd { 5 }")
	private final DoubleInput secondaryYAxisEnd;

	@Keyword(description = "The interval between secondary y-axis labels.",
	         example = "Graph1 SecondaryYAxisInterval { 1 }")
	private final DoubleInput secondaryYAxisInterval;

	@Keyword(description = "A list of time values between StartTime and EndTime where vertical gridlines are inserted.",
	         example = "Graph1 XLines { -48 -40 -32 -24 -16 -8 0 h }")
	private final ValueListInput xLines;

	@Keyword(description = "The color of the vertical gridlines (or a list corresponding to the colour of each " +
	                "gridline defined in XLines), defined using a colour keyword or RGB values.",
	         example = "Graph1 XLinesColor { gray76 }")
	private final ColorListInput xLinesColor;

	@Keyword(description = "Title of the y-axis, enclosed in single quotes, rotated by 90 degrees counter-clockwise.",
	         example = "Graph1 YAxisTitle { 'Water Height (m)' }")
	private final StringInput yAxisTitle; // Title for the y axis

	@Keyword(description = "Title of the secondary y-axis, enclosed in single quotes, rotated by 90 degrees clockwise.",
	         example = "Graph1 SecondaryYAxisTitle { 'Water Height (m)' }")
	private final StringInput secondaryYAxisTitle; // Title for the secondary y axis

	@Keyword(description= "Text for the graph title, enclosed in single quotes if it contains spaces.",
	         example = "Graph1 Title { 'Title of the Graph' }")
	private final StringInput title;	   // Title of the graph (shown on the center top)

	@Keyword(description = "The font name for all labels, enclosed in single quotes.",
	         example = "Graph1 LabelFontName { 'Arial' }")
	protected final StringInput labelFontName; // For all the texts
	protected Vec3d graphSize;   // graph size (the actual graph area)
	protected Vec3d graphOrigin; // bottom left position of the graph
	protected Vec3d graphCenter; // Center point of the graph

	// A list of the line thickness for corresponding item in targetEntityList

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.",
	         example = "Graph1 LineWidths { 1 2 3 7 }")
	protected final DoubleListInput lineWidths;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed.",
	         example = "Graph1 SecondaryLineWidths { 1 2 3 7 }")
	protected final DoubleListInput secondaryLineWidths;

	@Keyword(description = "A list of colours (each consisting of a colour keyword or RGB values) for the line series to be displayed. " +
	                "For multiple colours, each colour must be enclosed in braces as they can themselves be defined as a list of RGB values.",
	         example = "Graph1 LineColors { midnightblue }")
	protected final ColorListInput lineColorsList;	// list of line colours for each entity. Each element contains a vector of colours for each series for that entity
	//protected ArrayList<ColoringAttributes> lineColors;

	@Keyword(description = "A list of colours (each consisting of a colour keyword or RGB values) for the line series to be displayed. " +
	                "For multiple colours, each colour must be enclosed in braces as they can themselves be defined as a list of RGB values.",
	         example = "Graph1 SecondaryLineColors { midnightblue }")
	protected final ColorListInput secondaryLineColorsList;


	@Keyword(description = "The colour of the graph background, defined by a color keyword or RGB values.",
	         example = "Graph1 GraphColor { floralwhite }")
	private final ColourInput graphColor;

	@Keyword(description = "The colour for both axes labels, defined using a colour keyword or RGB values.",
	         example = "Graph1 LabelFontColor { black }")
	private final ColourInput labelFontColor;

	@Keyword(description = "The color for tick marks, defined using a colour keyword or RGB values.",
	         example = "Graph1 TickColor { black }")
	private final ColourInput tickColor;

	@Keyword(description = "The color for the outer pane background, defined using a colour keyword or RGB values.",
	         example = "Graph1 BackgroundColor { floralwhite }")
	private final ColourInput backgroundColor;

	@Keyword(description = "The color of the graph border, defined using a colour keyword or RGB values.",
	         example = "Graph1 BorderColor { red }")
	private final ColourInput borderColor;

	@Keyword(description = "The colour for the graph title, defined by a color keyword or RGB values.",
	         example = "Graph1 TitleColor { black }")
	private final ColourInput titleColor;


	@Keyword(description = "The text height for both x- and y-axis labels.",
	         example = "Graph1 LabelTextHeight { 0.35 }")
	private final DoubleInput labelTextHeight; // Text height for axes labels

	@Keyword(description = "The text height for the graph title.",
	         example = "Graph1 TitleTextHeight { 0.5 }")
	private final DoubleInput titleTextHeight; // Title text height

	@Keyword(description = "The text height for the y-axis title.",
	         example = "Graph1 YAxisTitleTextHeight { 0.3 }")
	private final DoubleInput yAxisTitleTextHeight; // y axis title text height

	@Keyword(description = "The gap between the x-axis labels and the x-axis.",
	         example = "Graph1 XAxisLabelGap { 0.3 }")
	private final DoubleInput xAxisLabelGap; // Distance between labels and x axis

	@Keyword(description = "The gap between the y-axis and its labels.  If left blank, this is automatically calculated.",
	         example = "Graph1 YAxisLabelGap { 0.3 }")
	private final DoubleInput yAxisLabelGap; // Distance between labels and y axis

	@Keyword(description = "The gap between the title and top of the graph.",
	         example = "Graph1 TitleGap { 0.3 }")
	private final DoubleInput titleGap; // gap between title and graph top

	@Keyword(description = "The gap between the y-axis title and the y-axis labels.",
	         example = "Graph1 yAxisTitleGap { 0.3 }")
	private final DoubleInput yAxisTitleGap; // gap between y axis title and y axis labels

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 TopMargin { 0.300 }")
	private final DoubleInput topMargin;		// Empty margin at top

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
             example = "Graph1 BottomMargin { 0.300 }")
	private final DoubleInput bottomMargin;	// Empty margin at bottom

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 LeftMargin { 0.200 }")
	private final DoubleInput leftMargin;		// Empty margin at left

	@Keyword(description = "The size of the margins from each of the four sides of the outer pane to the corresponding " +
	                "side of the graph.",
	         example = "Graph1 RightMargin { 0.400 }")
	private final DoubleInput rightMargin;		// Empty margin at right


	@Keyword(description = "Coordinates (in { x, y, z }) of the center of the legend.",
	         example = "Graph1 LegendCenter { -10 -10 0 }")
	private final Vec3dInput legendCenter; // TopLeft corner of the legend

	@Keyword(description = "Size (width and height) of the legend.",
	         example = "Graph1 LegendSize { 7.00 4.00 }")
	private final Vec3dInput legendSize;	  // size of legend

	@Keyword(description = "The height of the legend text.",
	         example = "Graph1 LegendTextHeight { 0.5 }")
	private DoubleInput legendTextHeight;			// height of text in legend

	@Keyword(description = "Width and height of the legend markers.",
	         example = "Graph1 LegendMarkerSize { 2.4 0.03 }")
	private final Vec3dInput legendMarkerSize;	// size of the marker for each series

	@Keyword(description = "The gap between the left margin of the legend and the text labels.",
	         example = "Graph1 LegendSeriesLabelGap { 3 }")
	private final DoubleInput seriesLabelGap;				// gap between the marker and the text for the series

	@Keyword(description = "The gap between the left margin of the legend and the legend markers.",
	         example = "Graph1 LegendSeriesMarkerGap { 0.1 }")
	private final DoubleInput seriesMakerGap;				// gap between the left border and the maker

	static int ENTITY_ONLY = 0;
	static int PARAMETER_ONLY = 1;
	static int ENTITY_PARAMETER = 2;


	@Keyword(description  = "The number of decimal places to show in the y-axis labels.",
	         example = "Graph1 YAxisPrecision { 1 }")
	private final IntegerInput yAxisPrecision; // number of decimal places to show in the y-axis labels

	@Keyword(description = "The number of decimal places to show in the secondary y-axis labels.",
	         example = "Graph1 SecondaryYAxisPrecision { 1 }")
	private final IntegerInput secondaryYAxisPrecision; // number of decimal places to show in the secondary y-axis labels



	@Keyword(description = "The Java format to be used for the tick mark values on the x-axis.\n" +
			"For example, the format %.1fs would dispaly the value 5 as 5.0s.",
	         example = "Graph1 XAxisLabelFormat { %.1fs }")
	private final StringInput xAxisLabelFormat;

	@Keyword(description = "The time unit to be used for the x-axis.",
	         example = "Graph1 XAxisUnit { h }")
	private final EntityInput<TimeUnit> xAxisUnit;
	@Keyword(description = "A numerical multiplier used to rescale the y-axis of a graph for different property value units." +
	         " Note: this only affects the display, the other inputs need to be specified in internal units",
	         example = "Graph1 YAxisMultiplier { 3.28083 }")
	private final DoubleInput yAxisMultiplier; // the value to multiply each y-axis label by

	@Keyword(description = "A numerical multiplier used to rescale the secondary y-axis of a graph for different property value units." +
	         " Note: this only affects the display, the other inputs need to be specified in internal units",
	         example = "Graph1 SecondaryYAxisMultiplier { 3.28083 }")
	private final DoubleInput secondaryYAxisMultiplier; // the value to multiply each secondary y-axis label by

	{
		dataSource = new OutputListInput<Double>(Double.class, "DataSource", "Data", null);
		this.addInput(dataSource, true);

		secondaryDataSource = new OutputListInput<Double>(Double.class, "SecondaryDataSource", "Secondary Data", null);
		this.addInput(secondaryDataSource, true);

		lineColorsList = new ColorListInput("LineColours", "Data", new ArrayList<Color4d>(0));
		this.addInput(lineColorsList, true, "LineColors");

		lineWidths = new DoubleListInput("LineWidths", "Data", new DoubleVector());
		this.addInput(lineWidths, true);

		numberOfPoints = new IntegerInput("NumberOfPoints", "Data", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints, true);

		startTime = new ValueInput("StartTime", "X Axis", -86400.0d);  // default = -24 hours
		startTime.setUnitType(TimeUnit.class);
		this.addInput(startTime, true);

		endTime = new ValueInput("EndTime", "X Axis", 0.0d);
		endTime.setUnitType(TimeUnit.class);
		this.addInput(endTime, true);

		timeInterval = new ValueInput("TimeInterval", "X Axis", 21600.0d); // default = 6 hours
		timeInterval.setUnitType(TimeUnit.class);
		this.addInput(timeInterval, true);

		xLines = new ValueListInput("XLines", "X Axis", new DoubleVector());
		xLines.setUnitType(TimeUnit.class);
		this.addInput(xLines, true);

		xLinesColor = new ColorListInput("XLinesColor", "X Axis", new ArrayList<Color4d>(0));
		this.addInput(xLinesColor, true, "XLinesColour");

		yAxisStart = new DoubleInput("YAxisStart", "Y Axis", 0.0);
		this.addInput(yAxisStart, true);

		yAxisEnd = new DoubleInput("YAxisEnd", "Y Axis", 5.0d);
		this.addInput(yAxisEnd, true);

		yAxisInterval = new DoubleInput("YAxisInterval", "Y Axis", 1.0d);
		this.addInput(yAxisInterval, true);

		yAxisMultiplier = new DoubleInput("YAxisMultiplier", "Y Axis", 1.0);
		this.addInput(yAxisMultiplier, true);

		yLines = new DoubleListInput("YLines", "Y Axis", new DoubleVector());
		this.addInput(yLines, true);

		yLinesColor = new ColorListInput("YLinesColor", "Y Axis", new ArrayList<Color4d>(0));
		this.addInput(yLinesColor, true, "YLinesColour");

		secondaryYAxisStart = new DoubleInput("SecondaryYAxisStart", "Y Axis", 0.0);
		this.addInput(secondaryYAxisStart, true);

		secondaryYAxisEnd = new DoubleInput("SecondaryYAxisEnd", "Y Axis", 0.0);
		this.addInput(secondaryYAxisEnd, true);

		secondaryYAxisInterval = new DoubleInput("SecondaryYAxisInterval", "Y Axis", 0.0);
		this.addInput(secondaryYAxisInterval, true);

		secondaryYAxisMultiplier = new DoubleInput("SecondaryYAxisMultiplier", "Y Axis", 1.0);
		this.addInput(secondaryYAxisMultiplier, true);


		labelTextHeight = new DoubleInput("LabelTextHeight", "X Axis Labels", 0.05);
		this.addInput(labelTextHeight, true);

		xAxisLabelGap = new DoubleInput("XAxisLabelGap", "X Axis Labels", 0.0);
		this.addInput(xAxisLabelGap, true);

		xAxisLabelFormat = new StringInput("XAxisLabelFormat", "X Axis Labels", "%.1f");
		this.addInput(xAxisLabelFormat, true);

		labelFontColor = new ColourInput("LabelFontColor", "X Axis Labels", ColourInput.BLUE);
		this.addInput(labelFontColor, true, "LabelFontColour");

		tickColor = new ColourInput("TickColor", "X Axis Labels", ColourInput.DARK_BLUE);
		this.addInput(tickColor, true, "TickColour");

		labelFontName = new StringInput("LabelFontName", "X Axis Labels", "Verdana");
		this.addInput(labelFontName, true);

		xAxisUnit = new EntityInput<TimeUnit>(TimeUnit.class, "XAxisUnit", "X Axis", null);
		this.addInput(xAxisUnit, true);

		yAxisTitleTextHeight = new DoubleInput("YAxisTitleTextHeight", "Y Axis Labels", 0.05d);
		this.addInput(yAxisTitleTextHeight, true);

		yAxisLabelGap = new DoubleInput("YAxisLabelGap", "Y Axis Labels", 0.0);
		this.addInput(yAxisLabelGap, true);

		yAxisTitleGap = new DoubleInput("YAxisTitleGap", "Y Axis Labels", 0.0);
		this.addInput(yAxisTitleGap, true);

		yAxisPrecision = new IntegerInput("YAxisPrecision", "Y Axis Labels", 0);
		yAxisPrecision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(yAxisPrecision, true);

		secondaryYAxisPrecision = new IntegerInput("SecondaryYAxisPrecision", "Y Axis Labels", 0);
		secondaryYAxisPrecision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(secondaryYAxisPrecision, true);

		yAxisTitle = new StringInput("YAxisTitle", "Y Axis Labels", "Y-Axis");
		this.addInput(yAxisTitle, true);

		secondaryYAxisTitle = new StringInput("SecondaryYAxisTitle", "Y Axis Labels", "");
		this.addInput(secondaryYAxisTitle, true);

		title = new StringInput("Title", "Title", "Title");
		this.addInput(title, true);

		titleTextHeight = new DoubleInput("TitleTextHeight", "Title", 0.0);
		this.addInput(titleTextHeight, true);

		titleColor = new ColourInput("TitleColor", "Title", ColourInput.getColorWithName("brick"));
		this.addInput(titleColor, true, "TitleColour");

		titleGap = new DoubleInput("TitleGap", "Title", 0.05d);
		this.addInput(titleGap, true);

		graphColor = new ColourInput("GraphColor", "Background", ColourInput.getColorWithName("ivory"));
		this.addInput(graphColor, true, "GraphColour");

		backgroundColor = new ColourInput("BackgroundColor", "Background", ColourInput.getColorWithName("gray95"));
		this.addInput(backgroundColor, true, "BackgroundColour");

		borderColor = new ColourInput("BorderColor", "Background", ColourInput.BLACK);
		this.addInput(borderColor, true, "BorderColour");

		topMargin = new DoubleInput("TopMargin", "Background", 0.20d);
		this.addInput(topMargin, true);

		bottomMargin = new DoubleInput("BottomMargin", "Background", 0.20d);
		this.addInput(bottomMargin, true);

		leftMargin = new DoubleInput("LeftMargin", "Background", 0.20d);
		this.addInput(leftMargin, true);

		rightMargin = new DoubleInput("RightMargin", "Background", 0.10d);
		this.addInput(rightMargin, true);

		legendTextHeight = new DoubleInput("LegendTextHeight", "Legend", 0.5);
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
		this.addInput(legendMarkerSize, true);

		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", "Secondary Data", new ArrayList<Color4d>(0));
		this.addInput(secondaryLineColorsList, true, "SecondaryLineColors");

		secondaryLineWidths = new DoubleListInput("SecondaryLineWidths", "Secondary Data", new DoubleVector());
		this.addInput(secondaryLineWidths, true);
	}

	public Graph() {

		primarySeries = new ArrayList<SeriesInfo>();
		secondarySeries = new ArrayList<SeriesInfo>();
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
		graphSize.x = ( ( graphExtent.x - ( leftMargin.getValue() +  rightMargin.getValue() ) ) / graphExtent.x );
		graphSize.y = ( ( graphExtent.y - (  topMargin.getValue() + bottomMargin.getValue() ) ) / graphExtent.y );
		graphSize.z = 1;

		// Center position of the graph
		graphCenter = new Vec3d( ( ( leftMargin.getValue() ) / 2 -	rightMargin.getValue()/2 ) / graphExtent.x,
				(( bottomMargin.getValue() ) / 2 - ( topMargin.getValue() ) / 2 ) / graphExtent.y , 0.0 );

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

	public double getLabelHeight() {
		return labelTextHeight.getValue() / getSize().y;
	}

	public double getTitleHeight() {
		double titleHeight = titleTextHeight.getValue();
		if( titleHeight == 0.0 && ! title.getValue().isEmpty() ) {
			titleHeight = labelTextHeight.getValue();
		}
		return titleHeight / getSize().y;
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

	public double getYAxisTitleHeight() {
		return yAxisTitleTextHeight.getValue() / getSize().x;
	}

	public double getYAxisTitleGap() {
		return yAxisTitleGap.getValue() / getSize().x;
	}

	public double getYAxisInterval() {
		return yAxisInterval.getValue();
	}
	public int getYAxisPrecision() {
		return yAxisPrecision.getValue();
	}

	public double getXAxisLabelGap() {
		return xAxisLabelGap.getValue() / getSize().y;
	}

	public double getSecondaryYAxisInterval() {
		return secondaryYAxisInterval.getValue();
	}
	public int getSecondaryYAxisPrecision() {
		return secondaryYAxisPrecision.getValue();
	}

	public double getYAxisLabelGap() {
		return yAxisLabelGap.getValue();
	}

	public double getTitleGap() {
		return titleGap.getValue();
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

	public double getYAxisMultiplier() {
		return yAxisMultiplier.getValue();
	}

	public double getTimeInterval() {
		return timeInterval.getValue();
	}

	public double getSecondaryYAxisMultiplier() {
		return secondaryYAxisMultiplier.getValue();
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
