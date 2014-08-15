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

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.OutputListInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class Graph extends GraphBasics  {

	// Key Inputs category

	@Keyword(description = "The number of data points that can be displayed on the graph.\n" +
			" This parameter determines the resolution of the graph.",
	         example = "Graph1 NumberOfPoints { 200 }")
	protected final IntegerInput numberOfPoints;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 DataSource { { Entity1 Output1 } { Entity2 Output2 } }")
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
	protected final ValueListInput lineWidths;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"Each source is graphed as a separate line and is specified by an Entity and its Output.",
     example = "Graph1 SecondaryDataSource { { Entity1 Output1 } { Entity2 Output2 } }")
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
	protected final ValueListInput secondaryLineWidths;

	{
		// Key Inputs category

		numberOfPoints = new IntegerInput("NumberOfPoints", "Key Inputs", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints);

		dataSource = new OutputListInput<Double>(Double.class, "DataSource", "Key Inputs", null);
		this.addInput(dataSource);

		ArrayList<Color4d> defLineColor = new ArrayList<Color4d>(0);
		defLineColor.add(ColourInput.getColorWithName("red"));
		lineColorsList = new ColorListInput("LineColours", "Key Inputs", defLineColor);
		lineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineColorsList);
		this.addSynonym(lineColorsList, "LineColors");

		DoubleVector defLineWidths = new DoubleVector(1);
		defLineWidths.add(1.0);
		lineWidths = new ValueListInput("LineWidths", "Key Inputs", defLineWidths);
		lineWidths.setUnitType(DimensionlessUnit.class);
		lineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(lineWidths);

		secondaryDataSource = new OutputListInput<Double>(Double.class, "SecondaryDataSource", "Key Inputs", null);
		this.addInput(secondaryDataSource);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<Color4d>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", "Key Inputs", defSecondaryLineColor);
		secondaryLineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineColorsList);
		this.addSynonym(secondaryLineColorsList, "SecondaryLineColors");

		DoubleVector defSecondaryLineWidths = new DoubleVector(1);
		defSecondaryLineWidths.add(1.0);
		secondaryLineWidths = new ValueListInput("SecondaryLineWidths", "Key Inputs", defSecondaryLineWidths);
		secondaryLineWidths.setUnitType(DimensionlessUnit.class);
		secondaryLineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondaryLineWidths);
	}

	public Graph() {

		timeTrace = true;
		this.setXAxisUnit(TimeUnit.class);
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
			this.setYAxisUnit(temp);
		}

		if (in == secondaryDataSource) {
			ArrayList<OutputHandle> outs = secondaryDataSource.getValue();
			showSecondaryYAxis = ! outs.isEmpty();
			if (outs.isEmpty())
				return;
			Class<? extends Unit> temp = outs.get(0).getUnitType();
			for (int i=1; i<outs.size(); i++) {
				if( outs.get(i).getUnitType() != temp )
					throw new InputErrorException("All inputs for keyword SecondaryDataSource must have the same unit type./n" +
							"The unit type for the first source is %s", temp);
			}
			this.setSecondaryYAxisUnit(temp);
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

		if (lineColorsList.getValue().size() > 1)
			Input.validateIndexedLists(dataSource, lineColorsList);

		if (secondaryLineColorsList.getValue().size() > 1)
			Input.validateIndexedLists(secondaryDataSource, secondaryLineColorsList);

		if (lineWidths.getValue().size() > 1)
			Input.validateIndexedLists(dataSource, lineWidths);

		if (secondaryLineWidths.getValue().size() > 1)
			Input.validateIndexedLists(secondaryDataSource, secondaryLineWidths);
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
			info.yValues = new double[numberOfPoints.getValue()];
			info.xValues = new double[numberOfPoints.getValue()];

			infos.add(info);
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


		double xLength = xAxisEnd.getValue() - xAxisStart.getValue();
		double xInterval = xLength/(numberOfPoints.getValue() -1);

		for (SeriesInfo info : primarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		for (SeriesInfo info : secondarySeries) {
			setupSeriesData(info, xLength, xInterval);
		}

		processGraph();
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

		for( int i = 0; i * xInterval < xAxisEnd.getValue(); i++ ) {
			double t = i * xInterval;
			info.numPoints++;
			info.xValues[info.numPoints] = t;
			info.yValues[info.numPoints] = this.getCurrentValue(t, info);
		}
	}

	/**
	 * A hook method for descendant graph types to grab some processing time
	 */
	protected void extraProcessing() {}

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

	private final ProcessTarget processGraph = new ProcessGraphTarget(this);

	/**
	 * Calculate values for the data series on the graph
	 */
	public void processGraph() {
		// Give processing time to sub-classes
		extraProcessing();

		// stop the processing loop
		if (primarySeries.isEmpty() && secondarySeries.isEmpty())
			return;

		// Calculate values for the primary y-axis
		for (SeriesInfo info : primarySeries) {
			processGraph(info);
		}

		// Calculate values for the secondary y-axis
		for (SeriesInfo info : secondarySeries) {
			processGraph(info);
		}

		double xLength = xAxisEnd.getValue() - xAxisStart.getValue();
		double xInterval = xLength / (numberOfPoints.getValue() - 1);
		scheduleProcess(xInterval, 7, processGraph);
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

		double t = getSimTime() + xAxisEnd.getValue();
		double presentValue = this.getCurrentValue(t, info);
		if (info.numPoints < info.yValues.length) {
			info.xValues[info.numPoints] = t;
			info.yValues[info.numPoints] = presentValue;
			info.numPoints++;
		}
		else {
			System.arraycopy(info.xValues, 1, info.xValues, 0, info.xValues.length - 1);
			System.arraycopy(info.yValues, 1, info.yValues, 0, info.yValues.length - 1);
			info.xValues[info.xValues.length - 1] = t;
			info.yValues[info.yValues.length - 1] = presentValue;
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

	public int getNumberOfPoints() {
		return numberOfPoints.getValue();
	}

}
