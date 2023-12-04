/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2012 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class Graph extends GraphBasics  {

	// Key Inputs category

	@Keyword(description = "The number of data points for each line on the graph.",
	         exampleList = {"200"})
	protected final SampleInput numberOfPoints;

	@Keyword(description = "The unit type for the primary y-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "One or more sources of data to be graphed against the primary y-axis. "
	                     + "Each source is graphed as a separate line. "
	                     + "BEFORE entering this input, specify the unit type for the primary "
	                     + "y-axis using the 'UnitType' keyword.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput dataSource;

	@Keyword(description = "A list of colours for the lines graphed against the primary y-axis. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput lineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the line series to be displayed. "
	                     + "If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput lineWidths;

	@Keyword(description = "The unit type for the secondary y-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput secondaryUnitType;

	@Keyword(description = "One or more sources of data to be graphed against the secondary y-axis. "
	                     + "Each source is graphed as a separate line. "
	                     + "BEFORE entering this input, specify the unit type for the secondary "
	                     + "y-axis using the 'SecondaryUnitType' keyword.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput secondaryDataSource;

	@Keyword(description = "A list of colours for the lines graphed against the secondary y-axis. "
	                     + "If only one colour is provided, it is used for all the lines.",
	         exampleList = {"{ red } { green }"})
	protected final ColorListInput secondaryLineColorsList;

	@Keyword(description = "A list of line widths (in pixels) for the seconardy line series to be displayed. "
	                     + "If only one line width is provided, it is used for all the lines.",
	         exampleList = {"2 1"})
	protected final ValueListInput secondaryLineWidths;

	{
		// Key Inputs category

		numberOfPoints = new SampleInput("NumberOfPoints", KEY_INPUTS, 100);
		numberOfPoints.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfPoints.setIntegerValue(true);
		this.addInput(numberOfPoints);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, DimensionlessUnit.class);
		unitType.setCallback(inputCallback1);
		this.addInput(unitType);

		dataSource = new SampleListInput("DataSource", KEY_INPUTS, null);
		dataSource.setUnitType(DimensionlessUnit.class);
		dataSource.setRequired(true);
		this.addInput(dataSource);

		ArrayList<Color4d> defLineColor = new ArrayList<>(0);
		defLineColor.add(ColourInput.getColorWithName("red"));
		lineColorsList = new ColorListInput("LineColours", KEY_INPUTS, defLineColor);
		lineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		lineColorsList.setCallback(inputCallback4);
		this.addInput(lineColorsList);
		this.addSynonym(lineColorsList, "LineColors");

		DoubleVector defLineWidths = new DoubleVector(1);
		defLineWidths.add(1.0);
		lineWidths = new ValueListInput("LineWidths", KEY_INPUTS, defLineWidths);
		lineWidths.setUnitType(DimensionlessUnit.class);
		lineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		lineWidths.setCallback(inputCallback5);
		this.addInput(lineWidths);

		secondaryUnitType = new UnitTypeInput("SecondaryUnitType", KEY_INPUTS, DimensionlessUnit.class);
		secondaryUnitType.setCallback(inputCallback2);
		this.addInput(secondaryUnitType);

		secondaryDataSource = new SampleListInput("SecondaryDataSource", KEY_INPUTS, null);
		secondaryDataSource.setUnitType(DimensionlessUnit.class);
		secondaryDataSource.setCallback(inputCallback3);
		this.addInput(secondaryDataSource);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondaryLineColorsList = new ColorListInput("SecondaryLineColours", KEY_INPUTS, defSecondaryLineColor);
		secondaryLineColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		secondaryLineColorsList.setCallback(inputCallback6);
		this.addInput(secondaryLineColorsList);
		this.addSynonym(secondaryLineColorsList, "SecondaryLineColors");

		DoubleVector defSecondaryLineWidths = new DoubleVector(1);
		defSecondaryLineWidths.add(1.0);
		secondaryLineWidths = new ValueListInput("SecondaryLineWidths", KEY_INPUTS, defSecondaryLineWidths);
		secondaryLineWidths.setUnitType(DimensionlessUnit.class);
		secondaryLineWidths.setValidCountRange(1, Integer.MAX_VALUE);
		secondaryLineWidths.setCallback(inputCallback7);
		this.addInput(secondaryLineWidths);
	}

	public Graph() {
		timeTrace = true;
		this.setXAxisUnit(TimeUnit.class);
		this.setYAxisUnit(DimensionlessUnit.class);
		this.setSecondaryYAxisUnit(DimensionlessUnit.class);
	}

	static final InputCallback inputCallback1 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue1();
		}
	};

	void updateInputValue1() {
		Class<? extends Unit> ut = unitType.getUnitType();
		dataSource.setUnitType(ut);
		this.setYAxisUnit(ut);
	}

	static final InputCallback inputCallback2 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue2();
		}
	};

	void updateInputValue2() {
		Class<? extends Unit> ut = secondaryUnitType.getUnitType();
		secondaryDataSource.setUnitType(ut);
		this.setSecondaryYAxisUnit(ut);
	}

	static final InputCallback inputCallback3 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue3();
		}
	};

	void updateInputValue3() {
		showSecondaryYAxis = !secondaryDataSource.isDefault();
	}

	static final InputCallback inputCallback4 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue4();
		}
	};

	void updateInputValue4() {
		for (int i = 0; i < primarySeries.size(); ++ i) {
			SeriesInfo info = primarySeries.get(i);
			info.lineColour = getLineColor(i, lineColorsList.getValue());
		}
	}

	static final InputCallback inputCallback5 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue5();
		}
	};

	void updateInputValue5() {
		for (int i = 0; i < primarySeries.size(); ++ i) {
			SeriesInfo info = primarySeries.get(i);
			info.lineWidth = getLineWidth(i, lineWidths.getValue());
		}
	}

	static final InputCallback inputCallback6 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue6();
		}
	};

	void updateInputValue6() {
		for (int i = 0; i < secondarySeries.size(); ++ i) {
			SeriesInfo info = secondarySeries.get(i);
			info.lineColour = getLineColor(i, secondaryLineColorsList.getValue());
		}
	}

	static final InputCallback inputCallback7 = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Graph)ent).updateInputValue7();
		}
	};

	void updateInputValue7() {
		for (int i = 0; i < secondarySeries.size(); ++ i) {
			SeriesInfo info = secondarySeries.get(i);
			info.lineWidth = getLineWidth(i, secondaryLineWidths.getValue());
		}
	}

	@Override
	public void setInputsForDragAndDrop() {}

	@Override
	public void earlyInit(){
		super.earlyInit();

		primarySeries.clear();
		secondarySeries.clear();

		// Populate the primary series data structures
		populateSeriesInfo(primarySeries, dataSource);
		populateSeriesInfo(secondarySeries, secondaryDataSource);
	}

	@Override
		public Vec3d getSize() {
		Vec3d ret = super.getSize();
		ret.z = Math.max(ret.z, 0.001d);
		return ret;
	}

	private void populateSeriesInfo(ArrayList<SeriesInfo> infos, SampleListInput data) {
		ArrayList<SampleProvider> sampList = data.getValue();
		if( sampList == null )
			return;
		int num = getNumberOfPoints();
		for (int i = 0; i < sampList.size(); ++i) {
			SeriesInfo info = new SeriesInfo();
			info.samp = sampList.get(i);
			info.yValues = new double[num];
			info.xValues = new double[num];

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
		double xInterval = xLength/(getNumberOfPoints() - 1);

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
		index = Math.min(index, colorList.size()-1);
		return colorList.get(index);
	}

	protected double getLineWidth(int index, DoubleVector widthList) {
		index = Math.min(index, widthList.size()-1);
		return widthList.get(index);
	}

	/**
	 * Initialize the data for the specified series
	 */
	private void setupSeriesData(SeriesInfo info, double xLength, double xInterval) {

		info.numPoints = 0;
		info.indexOfLastEntry = -1;

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
			return graph.getName() + ".processGraph";
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
		double xInterval = xLength / (getNumberOfPoints() - 1);
		EventManager.scheduleSeconds(xInterval, 7, false, processGraph, null);
	}

	/**
	 * Calculate values for the data series on the graph
	 * @param info - the information for the series to be rendered
	 */
	public void processGraph(SeriesInfo info) {

		// Entity has been removed
		if (info.samp == null) {
			return;
		}

		double t = EventManager.simSeconds() + xAxisEnd.getValue();
		double presentValue = this.getCurrentValue(t, info);

		info.indexOfLastEntry++;
		if (info.indexOfLastEntry == info.yValues.length) {
			info.indexOfLastEntry = 0;
		}

		info.xValues[info.indexOfLastEntry] = t;
		info.yValues[info.indexOfLastEntry] = presentValue;

		if (info.numPoints < info.yValues.length) {
			info.numPoints++;
		}
	}

	/**
	 * Return the current value for the series
	 * @return double
	 */
	protected double getCurrentValue(double simTime, SeriesInfo info) {
		return info.samp.getNextSample(this, simTime);
	}

	public ArrayList<SeriesInfo> getPrimarySeries() {
		return primarySeries;
	}

	public ArrayList<SeriesInfo> getSecondarySeries() {
		return secondarySeries;
	}

	public int getNumberOfPoints() {
		return (int) numberOfPoints.getNextSample(this, 0.0d);
	}

}
