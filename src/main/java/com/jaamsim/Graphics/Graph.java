/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2026 JaamSim Software Inc.
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
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class Graph extends AbstractGraph  {

	@Keyword(description = "The number of data points for each line on the graph.",
	         exampleList = {"200"})
	protected final SampleInput numberOfPoints;

	@Keyword(description = "One or more sources of data to be graphed against the primary y-axis. "
	                     + "Each source is graphed as a separate line. "
	                     + "BEFORE entering this input, specify the unit type for the primary "
	                     + "y-axis using the 'UnitType' keyword.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput dataSource;

	@Keyword(description = "One or more sources of data to be graphed against the secondary y-axis. "
	                     + "Each source is graphed as a separate line. "
	                     + "BEFORE entering this input, specify the unit type for the secondary "
	                     + "y-axis using the 'SecondaryUnitType' keyword.",
	         exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	protected final SampleListInput secondaryDataSource;

	{
		xAxisTitle.setDefaultValue("Time (h)");
		Unit unit = Input.parseEntity(getJaamSimModel(), "h", TimeUnit.class);
		xAxisUnit.setDefaultValue(unit);
		xAxisStart.setDefaultValue(-24 * 3600d);
		xAxisInterval.setDefaultValue(6 * 3600d);
		xLines.setDefaultValue(new DoubleVector(-6*3600d, -12*3600d, -18*3600d));

		xAxisStart.setValidRange(Double.NEGATIVE_INFINITY, 1.0e-6);
		xAxisEnd.setValidRange(0.0, Double.POSITIVE_INFINITY);

		numberOfPoints = new SampleInput("NumberOfPoints", KEY_INPUTS, 100);
		numberOfPoints.setValidRange(0, Double.POSITIVE_INFINITY);
		numberOfPoints.setIntegerValue(true);
		this.addInput(numberOfPoints);

		dataSource = new SampleListInput("DataSource", KEY_INPUTS, null);
		dataSource.setUnitType(DimensionlessUnit.class);
		dataSource.setRequired(true);
		this.addInput(dataSource);

		secondaryDataSource = new SampleListInput("SecondaryDataSource", KEY_INPUTS, null);
		secondaryDataSource.setUnitType(DimensionlessUnit.class);
		this.addInput(secondaryDataSource);
	}

	public Graph() {}

	@Override
	public void postDefine() {
		super.postDefine();
		setXAxisUnit(TimeUnit.class);
	}

	@Override
	protected void setYAxisUnit(Class<? extends Unit> unitType) {
		dataSource.setUnitType(unitType);
		super.setYAxisUnit(unitType);
	}

	@Override
	protected void setSecondaryYAxisUnit(Class<? extends Unit> unitType) {
		secondaryDataSource.setUnitType(unitType);
		super.setSecondaryYAxisUnit(unitType);
	}

	@Override
	public void setInputsForDragAndDrop() {}

	@Override
	public void earlyInit(){
		super.earlyInit();

		// Populate the primary series data structures
		int num = getNumberOfPoints();
		populatePrimarySeriesInfo(dataSource.getListSize(), num, dataSource.getValue());
		populateSecondarySeriesInfo(secondaryDataSource.getListSize(), num, secondaryDataSource.getValue());
	}

	@Override
		public Vec3d getSize() {
		Vec3d ret = super.getSize();
		ret.z = Math.max(ret.z, 0.001d);
		return ret;
	}

	@Override
	public void startUp() {
		super.startUp();
		extraStartGraph();

		for (int i = 0; i < primarySeriesSize(); ++ i) {
			setPrimarySeriesColour(i, getLineColor(i));
			setPrimarySeriesWidth(i, getLineWidth(i));
		}

		for (int i = 0; i < secondarySeriesSize(); ++i) {
			setSecondarySeriesColour(i, getSecondaryLineColor(i));
			setSecondarySeriesWidth(i, getSecondaryLineWidth(i));
		}

		double xLength = xAxisEnd.getValue() - xAxisStart.getValue();
		double xInterval = xLength/(getNumberOfPoints() - 1);

		for (SeriesInfo info : getPrimarySeries()) {
			setupSeriesData(info, xLength, xInterval);
		}

		for (SeriesInfo info : getSecondarySeries()) {
			setupSeriesData(info, xLength, xInterval);
		}

		processGraph();
	}

	/**
	 * Hook for sub-classes to do some processing at startup
	 */
	protected void extraStartGraph() {}

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
		ArrayList<SeriesInfo> primarySeries = getPrimarySeries();
		ArrayList<SeriesInfo> secondarySeries = getSecondarySeries();
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
		EventManager.scheduleSeconds(xInterval, PRI_MED_LOW, EVT_LIFO, processGraph, null);
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

		double t = getSimTime() + xAxisEnd.getValue();
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

	public int getNumberOfPoints() {
		return (int) numberOfPoints.getNextSample(this, 0.0d);
	}

	@Override
	public boolean showSecondaryYAxis() {
		return !secondaryDataSource.isDefault();
	}

	@Override
	public boolean isTimeTrace() {
		return true;
	}

}
