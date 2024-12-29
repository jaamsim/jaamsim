/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023-2024 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpressionListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class XYGraph extends AbstractGraph {

	@Keyword(description = "Unit type for the x-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput xUnitType;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an "
	                     + "array of numbers with or without units.",
	         exampleList = {"{ [Statistics1].HistogramBinFractions } { [Statistics2].HistogramBinFractions }"})
	protected final ExpressionListInput yDataSource;

	@Keyword(description = "One or more sources of data for the x-axis values corresponding to the "
	                     + "primary y-axis data sources.\n"
	                     + "Each source is specified by an array of numbers with or without units.",
	         exampleList = {"{ [Statistics1].HistogramBinCentres } { [Statistics2].HistogramBinCentres }"})
	protected final ExpressionListInput xDataSource;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an "
	                     + "array of numbers with or without units.",
	         exampleList = {"{ [Statistics1].HistogramBinCumulativeFractions } { [Statistics2].HistogramBinCumulativeFractions }"})
	protected final ExpressionListInput ySecondaryDataSource;

	@Keyword(description = "One or more sources of data for the x-axis values corresponding to the "
	                     + "secondary y-axis data sources.\n"
	                     + "Each source is specified by an array of numbers with or without units.",
	         exampleList = {"{ [Statistics1].HistogramBinUpperLimits } { [Statistics2].HistogramBinUpperLimits }"})
	protected final ExpressionListInput xSecondaryDataSource;

	@Keyword(description = "Type of graph for each of the primary series:\n"
	                     + "LINE_GRAPH - each series displayed as a line\n"
	                     + "BAR_GRAPH  - each series displayed as a sequence of bars")
	protected final EnumInput<ValidGraphTypes> graphType;

	@Keyword(description = "Type of graph for each of the secondary series:\n"
	                     + "LINE_GRAPH - each series displayed as a line\n"
	                     + "BAR_GRAPH  - each series displayed as a sequence of bars")
	protected final EnumInput<ValidGraphTypes> secondaryGraphType;

	enum ValidGraphTypes {
		LINE_GRAPH,
		BAR_GRAPH,
	}

	{
		xAxisStart.setDefaultValue(0.0d);
		xAxisEnd.setDefaultValue(10.0d);
		xAxisInterval.setDefaultValue(1.0d);
		xAxisLabelFormat.setDefaultValue("%.0f");
		xLines.setDefaultValue(new DoubleVector(5.0d));

		xUnitType = new UnitTypeInput("XAxisUnitType", KEY_INPUTS, DimensionlessUnit.class);
		xUnitType.setCallback(xAxisUnitTypeCallback);
		this.addInput(xUnitType);

		yDataSource = new ExpressionListInput("YDataSource", KEY_INPUTS, null);
		yDataSource.setResultType(ExpResType.COLLECTION);
		yDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yDataSource);

		xDataSource = new ExpressionListInput("XDataSource", KEY_INPUTS, null);
		xDataSource.setResultType(ExpResType.COLLECTION);
		xDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xDataSource);

		ySecondaryDataSource = new ExpressionListInput("YSecondaryDataSource", KEY_INPUTS, null);
		ySecondaryDataSource.setResultType(ExpResType.COLLECTION);
		ySecondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(ySecondaryDataSource);

		xSecondaryDataSource = new ExpressionListInput("XSecondaryDataSource", KEY_INPUTS, null);
		xSecondaryDataSource.setResultType(ExpResType.COLLECTION);
		xSecondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xSecondaryDataSource);

		graphType = new EnumInput<>(ValidGraphTypes.class, "GraphType", FORMAT, ValidGraphTypes.LINE_GRAPH);
		addInput(graphType);

		secondaryGraphType = new EnumInput<>(ValidGraphTypes.class, "SecondaryGraphType", FORMAT, ValidGraphTypes.LINE_GRAPH);
		addInput(secondaryGraphType);
	}

	public XYGraph() {}

	static final InputCallback xAxisUnitTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Class<? extends Unit> ut = ((UnitTypeInput) inp).getUnitType();
			((XYGraph) ent).setXAxisUnit(ut);
		}
	};

	@Override
	public void setInputsForDragAndDrop() {}

	@Override
	protected void setYAxisUnit(Class<? extends Unit> unitType) {
		super.setYAxisUnit(unitType);
		yDataSource.setUnitType(unitType);
		updateUserOutputMap();
	}

	@Override
	protected void setSecondaryYAxisUnit(Class<? extends Unit> unitType) {
		super.setSecondaryYAxisUnit(unitType);
		ySecondaryDataSource.setUnitType(unitType);
		updateUserOutputMap();
	}

	@Override
	protected void setXAxisUnit(Class<? extends Unit> unitType) {
		super.setXAxisUnit(unitType);
		xDataSource.setUnitType(unitType);
	}

	@Override
	public boolean showSecondaryYAxis() {
		return !ySecondaryDataSource.isDefault();
	}

	@Override
	public void updateGraphics(double simTime){
		super.updateGraphics(simTime);

		getPrimarySeries().clear();
		getSecondarySeries().clear();

		if (!yDataSource.isDefault() && !xDataSource.isDefault()) {
			int numSeries = Math.min(yDataSource.getListSize(), xDataSource.getListSize());
			for (int series = 0; series < numSeries; series++) {
				ExpResult.Collection yCol = yDataSource.getNextResult(series, this, simTime).colVal;
				ExpResult.Collection xCol = xDataSource.getNextResult(series, this, simTime).colVal;

				SeriesInfo info = new SeriesInfo();
				getPrimarySeries().add(info);
				int numPoints = Math.min(xCol.getSize(), yCol.getSize());
				info.yValues = new double[numPoints];
				info.xValues = new double[numPoints];

				info.numPoints = numPoints;
				info.indexOfLastEntry = numPoints - 1;
				info.lineColour = getLineColor(series);
				info.lineWidth = getLineWidth(series);
				info.isBar = (graphType.getValue() == ValidGraphTypes.BAR_GRAPH);
				for (int i = 0; i < info.numPoints; i++ ) {
					ExpResult ind = ExpResult.makeNumResult(i + 1, DimensionlessUnit.class);
					try {
						info.yValues[i] = yCol.index(ind).value;
						info.xValues[i] = xCol.index(ind).value;
					}
					catch (ExpError e) {}
				}
				//System.out.format("numPoints=%s, yValues=%s, xValues=%s%n", info.numPoints,
				//		Arrays.toString(info.yValues), Arrays.toString(info.xValues));
			}
		}

		if (!ySecondaryDataSource.isDefault() && !xSecondaryDataSource.isDefault()) {
			int numSeries = Math.min(ySecondaryDataSource.getListSize(), xSecondaryDataSource.getListSize());
			for (int series = 0; series < numSeries; series++) {
				ExpResult.Collection ySecCol = ySecondaryDataSource.getNextResult(series, this, simTime).colVal;
				ExpResult.Collection xSecCol = xSecondaryDataSource.getNextResult(series, this, simTime).colVal;

				SeriesInfo info = new SeriesInfo();
				getSecondarySeries().add(info);
				int numPointsSec = Math.min(xSecCol.getSize(), ySecCol.getSize());
				info.yValues = new double[numPointsSec];
				info.xValues = new double[numPointsSec];

				info.numPoints = numPointsSec;
				info.indexOfLastEntry = numPointsSec - 1;
				info.lineColour = getSecondaryLineColor(series);
				info.lineWidth = getSecondaryLineWidth(series);
				info.isBar = (secondaryGraphType.getValue() == ValidGraphTypes.BAR_GRAPH);
				for (int i = 0; i < info.numPoints; i++ ) {
					ExpResult ind = ExpResult.makeNumResult(i + 1, DimensionlessUnit.class);
					try {
						info.yValues[i] = ySecCol.index(ind).value;
						info.xValues[i] = xSecCol.index(ind).value;
					}
					catch (ExpError e) {}
				}
			}
		}
	}

}
